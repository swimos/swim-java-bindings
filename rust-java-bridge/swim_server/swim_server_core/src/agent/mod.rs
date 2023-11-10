use std::collections::HashMap;
use std::fmt::{Debug, Formatter};
use std::future::Future;
use std::mem::size_of;
use std::ops::ControlFlow;
use std::pin::Pin;
use std::task::{ready, Context, Poll};

use bytes::BytesMut;
use futures::Stream;
use futures::{pin_mut, StreamExt};
use futures_util::future::BoxFuture;
use futures_util::stream::{unfold, SelectAll};
use futures_util::{FutureExt, SinkExt};
use swim_api::agent::{Agent, AgentConfig, AgentContext, AgentInitResult};
use swim_api::error::{AgentInitError, AgentRuntimeError, AgentTaskError, FrameIoError};
use swim_api::meta::lane::LaneKind;
use swim_api::protocol::agent::{
    LaneRequest, StoreInitMessage, StoreInitMessageDecoder, StoreInitialized, StoreInitializedCodec,
};
use swim_api::protocol::WithLengthBytesCodec;
use swim_model::Text;
use swim_utilities::future::try_last;
use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
use swim_utilities::routing::route_uri::RouteUri;
use tokio::io::AsyncWriteExt;
use tokio::sync::mpsc;
use tokio::{pin, select};
use tokio_util::codec::{Decoder, FramedRead, FramedWrite};
use tokio_util::time::delay_queue::Key;
use tracing::{debug, info, span, trace, Level};
use tracing_futures::Instrument;
use uuid::Uuid;

use interval_stream::{IntervalStream, ItemStatus, ScheduleDef, StreamItem};

use crate::agent::foreign::{GuestAgentFactory, GuestAgentVTable, GuestRuntimeContext};
use crate::agent::spec::{AgentSpec, LaneSpec};
use crate::codec::{LaneReaderCodec, LaneResponseDecoder, LaneResponseElement};

#[cfg(test)]
mod tests;

pub mod context;
pub mod foreign;
pub mod spec;

#[derive(Debug, Clone)]
pub struct JavaGuestAgent<C, F> {
    guest_context: C,
    spec: AgentSpec,
    agent_factory: F,
    guest_config: JavaGuestConfig,
}

impl<C, F> JavaGuestAgent<C, F> {
    pub fn new(
        guest_context: C,
        spec: AgentSpec,
        agent_factory: F,
        guest_config: JavaGuestConfig,
    ) -> JavaGuestAgent<C, F> {
        JavaGuestAgent {
            guest_context,
            spec,
            agent_factory,
            guest_config,
        }
    }
}

impl<C, F> Agent for JavaGuestAgent<C, F>
where
    F: GuestAgentFactory<Context = C>,
    C: GuestRuntimeContext,
{
    fn run(
        &self,
        route: RouteUri,
        route_params: HashMap<String, String>,
        config: AgentConfig,
        context: Box<dyn AgentContext + Send>,
    ) -> BoxFuture<'static, AgentInitResult> {
        let JavaGuestAgent {
            guest_config,
            guest_context,
            spec,
            agent_factory,
        } = self;

        // todo: channel size from config/replace with a SPSC channel
        let (guest_tx, guest_rx) = mpsc::channel(8);

        let uri_string = route.to_string();
        let create_result =
            agent_factory.agent_for(guest_context.clone(), uri_string.as_str(), guest_tx);
        let guest_context = guest_context.clone();
        let spec = spec.clone();
        let guest_config = *guest_config;

        let task = async move {
            match create_result {
                Ok(agent) => initialize_agent(
                    guest_config,
                    guest_context,
                    spec,
                    route,
                    route_params,
                    config,
                    context,
                    agent,
                    guest_rx,
                )
                .await
                .map(|task| task.run().boxed()),
                Err(e) => Err(e),
            }
        };

        task.instrument(span!(Level::DEBUG, "Agent task", uri_string))
            .boxed()
    }
}

enum OpenLaneError {
    AgentRuntime(AgentRuntimeError),
    FrameIo(FrameIoError),
}

impl From<AgentRuntimeError> for OpenLaneError {
    fn from(value: AgentRuntimeError) -> Self {
        OpenLaneError::AgentRuntime(value)
    }
}

impl From<FrameIoError> for OpenLaneError {
    fn from(value: FrameIoError) -> Self {
        OpenLaneError::FrameIo(value)
    }
}

async fn open_lane<A>(
    spec: LaneSpec,
    uri: &str,
    context: &Box<dyn AgentContext + Send>,
    agent_environment: &mut GuestEnvironment<A>,
) -> Result<(), OpenLaneError>
where
    A: GuestAgentVTable,
{
    let GuestEnvironment {
        config,
        lane_identifiers,
        guest_agent,
        lane_readers,
        lane_writers,
        ..
    } = agent_environment;
    let LaneSpec {
        is_transient,
        lane_idx,
        lane_kind_repr,
    } = spec;

    let kind: LaneKind = lane_kind_repr.into();
    debug!(uri, lane_kind = ?lane_kind_repr, is_transient, "Adding lane");

    let text_uri: Text = uri.into();
    let mut lane_conf = config.default_lane_config.unwrap_or_default();
    lane_conf.transient = is_transient;

    let (tx, rx) = context.add_lane(text_uri.as_str(), kind, lane_conf).await?;

    if is_transient {
        let reader = if lane_kind_repr.map_like() {
            LaneReader::map(lane_idx, rx)
        } else {
            LaneReader::value(lane_idx, rx)
        };
        lane_readers.push(reader);
        lane_writers.insert(lane_idx, tx);
    } else {
        let (reader, id, tx) = if lane_kind_repr.map_like() {
            let InitializedLane { io: (tx, rx), id } = run_lane_initializer(
                MapLikeLaneGuestInitializer::new(guest_agent, spec.lane_idx),
                (tx, rx),
                WithLengthBytesCodec::default(),
                lane_idx,
            )
            .await?;

            (LaneReader::map(id, rx), id, tx)
        } else {
            let InitializedLane { io: (tx, rx), id } = run_lane_initializer(
                ValueLikeLaneGuestInitializer::new(guest_agent, spec.lane_idx),
                (tx, rx),
                WithLengthBytesCodec::default(),
                lane_idx,
            )
            .await?;
            (LaneReader::value(id, rx), id, tx)
        };

        lane_readers.push(reader);
        lane_writers.insert(id, tx);
    }

    lane_identifiers.insert(spec.lane_idx, text_uri);

    Ok(())
}

async fn initialize_agent<A, C>(
    guest_config: JavaGuestConfig,
    guest_context: C,
    spec: AgentSpec,
    route: RouteUri,
    route_params: HashMap<String, String>,
    config: AgentConfig,
    agent_context: Box<dyn AgentContext + Send>,
    guest_agent: A,
    runtime_requests: mpsc::Receiver<GuestRuntimeRequest>,
) -> Result<GuestAgentTask<A, C>, AgentInitError>
where
    A: GuestAgentVTable,
{
    let AgentSpec { lane_specs, .. } = spec;

    let mut agent_environment = GuestEnvironment {
        guest_config,
        _route: route,
        _route_params: route_params,
        config,
        lane_identifiers: HashMap::new(),
        guest_agent,
        lane_readers: SelectAll::new(),
        lane_writers: HashMap::new(),
    };

    for (uri, spec) in lane_specs.into_iter() {
        match open_lane(spec, uri.as_str(), &agent_context, &mut agent_environment).await {
            Ok(()) => continue,
            Err(OpenLaneError::AgentRuntime(error)) => {
                return match error {
                    AgentRuntimeError::Stopping => Err(AgentInitError::FailedToStart),
                    AgentRuntimeError::Terminated => Err(AgentInitError::FailedToStart),
                }
            }
            Err(OpenLaneError::FrameIo(error)) => {
                return Err(AgentInitError::LaneInitializationFailure(error));
            }
        }
    }

    debug!("Agent initialised");

    Ok(GuestAgentTask {
        guest_context,
        agent_context,
        agent_environment,
        runtime_requests,
    })
}

struct InitializedLane {
    io: (ByteWriter, ByteReader),
    id: i32,
}

fn init_stream<'a, D>(
    reader: &'a mut ByteReader,
    decoder: D,
) -> impl Stream<Item = Result<D::Item, FrameIoError>> + 'a
where
    D: Decoder + 'a,
    FrameIoError: From<D::Error>,
{
    let framed = FramedRead::new(reader, StoreInitMessageDecoder::new(decoder));
    unfold(Some(framed), |maybe_framed| async move {
        if let Some(mut framed) = maybe_framed {
            match framed.next().await {
                Some(Ok(StoreInitMessage::Command(body))) => Some((Ok(body), Some(framed))),
                Some(Ok(StoreInitMessage::InitComplete)) => None,
                Some(Err(e)) => Some((Err(e), None)),
                None => Some((Err(FrameIoError::InvalidTermination), None)),
            }
        } else {
            None
        }
    })
}

trait GuestItemInitializer {
    fn initialize<'s, S>(&'s mut self, stream: S) -> BoxFuture<Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 's;
}

struct ValueLikeLaneGuestInitializer<'a, A> {
    agent_obj: &'a mut A,
    lane_id: i32,
}

impl<'a, A> ValueLikeLaneGuestInitializer<'a, A> {
    pub fn new(agent_obj: &'a mut A, lane_id: i32) -> ValueLikeLaneGuestInitializer<'a, A> {
        ValueLikeLaneGuestInitializer { agent_obj, lane_id }
    }
}

impl<'a, A> GuestItemInitializer for ValueLikeLaneGuestInitializer<'a, A>
where
    A: GuestAgentVTable,
{
    fn initialize<'s, S>(&'s mut self, stream: S) -> BoxFuture<Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 's,
    {
        println!("Running ValueLikeLaneGuestInitializer");
        let ValueLikeLaneGuestInitializer { agent_obj, lane_id } = self;
        Box::pin(async move {
            match try_last(stream).await? {
                Some(body) => {
                    // todo: add a new variant to FrameIoError to handle this
                    agent_obj.init(*lane_id, body).await.expect("Init error");
                    Ok(())
                }
                None => Ok(()),
            }
        })
    }
}

struct MapLikeLaneGuestInitializer<'a, A> {
    agent_obj: &'a mut A,
    lane_id: i32,
}

impl<'a, A> MapLikeLaneGuestInitializer<'a, A> {
    pub fn new(agent_obj: &'a mut A, lane_id: i32) -> MapLikeLaneGuestInitializer<'a, A> {
        MapLikeLaneGuestInitializer { agent_obj, lane_id }
    }
}

impl<'a, A> GuestItemInitializer for MapLikeLaneGuestInitializer<'a, A>
where
    A: GuestAgentVTable,
{
    fn initialize<'s, S>(&'s mut self, stream: S) -> BoxFuture<Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 's,
    {
        let MapLikeLaneGuestInitializer { agent_obj, lane_id } = self;
        Box::pin(async move {
            pin_mut!(stream);
            // Event buffer to reduce the number of FFI calls to initialise the lane.
            let mut buf = BytesMut::new();

            while let Some(result) = stream.next().await {
                match result {
                    Ok(ev) => {
                        if buf.len() + ev.len() < size_of::<i32>() {
                            buf.extend_from_slice(ev.as_ref());
                        } else {
                            agent_obj.init(*lane_id, buf);
                            buf = BytesMut::new();
                        }
                    }
                    Err(e) => return Err(e),
                }
            }

            if !buf.is_empty() {
                agent_obj.init(*lane_id, buf);
            }

            Ok(())
        })
    }
}

async fn run_lane_initializer<'a, I, D>(
    mut initializer: I,
    io: (ByteWriter, ByteReader),
    decoder: D,
    id: i32,
) -> Result<InitializedLane, FrameIoError>
where
    D: Decoder<Item = BytesMut> + Send,
    I: GuestItemInitializer,
    FrameIoError: From<D::Error>,
{
    let (mut tx, mut rx) = io;
    let stream = init_stream(&mut rx, decoder);
    match initializer.initialize(stream).await {
        Ok(()) => {
            let mut writer = FramedWrite::new(&mut tx, StoreInitializedCodec);
            writer
                .send(StoreInitialized)
                .await
                .map_err(|e| FrameIoError::Io(e))
                .map(move |_| InitializedLane { io: (tx, rx), id })
        }
        Err(e) => Err(e),
    }
}

#[derive(Debug)]
enum RuntimeEvent {
    Request {
        id: i32,
        request: LaneRequest<BytesMut>,
    },
    RequestError {
        id: i32,
        error: FrameIoError,
    },
    ScheduledEvent {
        event: TaskState,
    },
}

#[derive(Clone, Debug)]
struct TaskDef {
    id: Uuid,
}

/// Guest language configuration.
#[derive(Debug, Copy, Clone, PartialEq)]
pub struct JavaGuestConfig {
    /// The maximum message size that the guest language is capable of processing.
    ///
    /// For Java, this is ~ 2 ^ size_of::<i32>() - 8.
    max_message_size: u64,
}

impl JavaGuestConfig {
    pub fn java_default() -> JavaGuestConfig {
        JavaGuestConfig {
            // This is the maximum array size that Java can handle. It varies by VM so it may be
            // better to reflect ArrayList#MAX_ARRAY_SIZE and provide it at runtime.
            max_message_size: (2 ^ size_of::<i32>() - 8) as u64,
        }
    }
}

struct GuestEnvironment<V> {
    guest_config: JavaGuestConfig,
    guest_agent: V,
    _route: RouteUri,
    _route_params: HashMap<String, String>,
    config: AgentConfig,
    lane_identifiers: HashMap<i32, Text>,
    lane_readers: SelectAll<LaneReader>,
    lane_writers: HashMap<i32, ByteWriter>,
}

struct GuestAgentTask<A, C> {
    guest_context: C,
    agent_environment: GuestEnvironment<A>,
    runtime_requests: mpsc::Receiver<GuestRuntimeRequest>,
    agent_context: Box<dyn AgentContext + Send>,
}

impl<A, C> GuestAgentTask<A, C> {
    async fn run(self) -> Result<(), AgentTaskError>
    where
        A: GuestAgentVTable,
        C: GuestRuntimeContext,
    {
        let GuestAgentTask {
            guest_context,
            mut agent_environment,
            mut runtime_requests,
            agent_context,
        } = self;

        info!("Running agent");

        let mut task_scheduler = TaskScheduler::default();
        let max_message_size = agent_environment.guest_config.max_message_size;

        let did_start_call = agent_environment.guest_agent.did_start();

        if let ControlFlow::Break(()) = suspend(
            &mut agent_environment,
            &agent_context,
            &mut runtime_requests,
            &mut task_scheduler,
            &guest_context,
            did_start_call,
        )
        .await?
        {
            return Ok(());
        }

        loop {
            let event: Option<RuntimeEvent> = if task_scheduler.is_empty() {
                select! {
                    message = agent_environment.lane_readers.next() => message.map(|(id, request)| {
                        match request {
                            Ok(request) => RuntimeEvent::Request { id, request },
                            Err(error) => RuntimeEvent::RequestError { id, error }
                        }
                    }),
                }
            } else {
                select! {
                    message = agent_environment.lane_readers.next() => message.map(|(id, request)| {
                        match request {
                            Ok(request) => RuntimeEvent::Request { id, request },
                            Err(error) => RuntimeEvent::RequestError { id, error }
                        }
                    }),
                    task = task_scheduler.next() => task.map(|event| RuntimeEvent::ScheduledEvent { event })
                }
            };

            debug!(event = ?event, "Guest agent task received runtime event");

            match event {
                Some(RuntimeEvent::Request { id, request }) => {
                    let handle = match request {
                        LaneRequest::Command(msg) => {
                            if msg.len() as u64 > max_message_size {
                                continue;
                            }

                            trace!("Received a command request");
                            agent_environment.guest_agent.dispatch(id, msg)
                        }
                        LaneRequest::Sync(remote_id) => {
                            trace!("Received a sync request");
                            agent_environment.guest_agent.sync(id, remote_id)
                        }
                        LaneRequest::InitComplete => continue,
                    };

                    if let ControlFlow::Break(()) = suspend(
                        &mut agent_environment,
                        &agent_context,
                        &mut runtime_requests,
                        &mut task_scheduler,
                        &guest_context,
                        handle,
                    )
                    .await?
                    {
                        return Ok(());
                    }
                }
                Some(RuntimeEvent::RequestError { id, error }) => {
                    let lane = agent_environment
                        .lane_identifiers
                        .get(&id)
                        .cloned()
                        .expect("Missing lane");
                    return Err(AgentTaskError::BadFrame { lane, error });
                }
                None => break,
                Some(RuntimeEvent::ScheduledEvent { event }) => {
                    let TaskState { id, complete } = event;
                    let handle = agent_environment.guest_agent.run_task(id, complete);
                    if let ControlFlow::Break(()) = suspend(
                        &mut agent_environment,
                        &agent_context,
                        &mut runtime_requests,
                        &mut task_scheduler,
                        &guest_context,
                        handle,
                    )
                    .await?
                    {
                        return Ok(());
                    }
                }
            }
        }

        let did_stop_call = agent_environment.guest_agent.did_stop();
        if let ControlFlow::Break(()) = suspend(
            &mut agent_environment,
            &agent_context,
            &mut runtime_requests,
            &mut task_scheduler,
            &guest_context,
            did_stop_call,
        )
        .await?
        {
            return Ok(());
        }

        Ok(())
    }
}

enum SuspendedRuntimeEvent {
    Request(Option<GuestRuntimeRequest>),
    SuspendComplete(Result<Vec<u8>, AgentTaskError>),
}

impl Debug for SuspendedRuntimeEvent {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            SuspendedRuntimeEvent::Request(r) => {
                write!(f, "SuspendedRuntimeEvent::Request({:?})", r)
            }
            SuspendedRuntimeEvent::SuspendComplete(Ok(_)) => {
                write!(f, "SuspendedRuntimeEvent::SuspendComplete(Ok(_))")
            }
            SuspendedRuntimeEvent::SuspendComplete(Err(e)) => {
                write!(f, "SuspendedRuntimeEvent::SuspendComplete(Err({e:?}))")
            }
        }
    }
}

/// Scheduler for delaying tasks to be run in a guest runtime. Task ID's are pushed into this
/// scheduler with an associated schedule at which the IDs will be yielded at.
#[derive(Default)]
struct TaskScheduler {
    /// The queue of delayed tasks.
    tasks: IntervalStream<TaskDef>,
    /// Map of task IDs and their associated keys in the `IntervalStream's` `DelayQueue`. This is
    /// required as task keys may change each time the task is reinserted back in to the `IntervalStream`
    /// after it has been run.   
    keys: HashMap<Uuid, Key>,
}

impl TaskScheduler {
    /// Returns true if there are no tasks scheduled.
    fn is_empty(&self) -> bool {
        self.tasks.is_empty()
    }

    /// Schedules a new task to be run using the provided schedule.
    ///
    /// # Arguments:
    /// - `id`: the ID of the task registered in the guest runtime.
    /// - `schedule`: the schedule to yield the tasks at.
    fn push_task(&mut self, id: Uuid, schedule: ScheduleDef) {
        let TaskScheduler { tasks, keys } = self;
        let key = tasks.push(schedule, TaskDef { id });
        keys.insert(id, key);
    }

    /// Cancels the task associated with `id`.
    fn cancel_task(&mut self, id: Uuid) {
        let TaskScheduler { tasks, keys } = self;
        match keys.get(&id) {
            Some(key) => {
                tasks.remove(key);
            }
            None => {
                panic!("Missing key for task ID: {}", id)
            }
        }
    }
}

#[derive(Clone, Debug)]
struct TaskState {
    id: Uuid,
    complete: bool,
}

impl Stream for TaskScheduler {
    type Item = TaskState;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let status = ready!(Pin::new(&mut self.tasks).poll_next(cx));
        match status {
            Some(item) => {
                let StreamItem { item, status } = item;
                let id = item.id;
                match status {
                    ItemStatus::Complete => {
                        self.keys.remove(&id);
                        Poll::Ready(Some(TaskState { id, complete: true }))
                    }
                    ItemStatus::WillYield { key } => {
                        self.keys.insert(id, key);
                        Poll::Ready(Some(TaskState {
                            id,
                            complete: false,
                        }))
                    }
                }
            }
            None => Poll::Ready(None),
        }
    }
}

/// Suspends the runtime and awaits any requests from the guest language. No incoming requests from
/// the Rust runtime are processed until the suspended function has completed.
///
/// # Arguments
/// `agent_environment`: the state of the agent and its associated configuration.
/// `agent_context`: the context that has been provided to the agent.
/// `runtime_requests`: the channel that the guest language can use to make requests to the Rust
/// runtime. If this channel is dropped then the agent will stop.
/// `task_scheduler`: a scheduler for suspending tasks. Any requests made by the guest language to
/// suspend a task will be inserted into this.
/// `suspended_task`: the suspended function. Once this has completed, `followed_by` will be invoked
/// with the output of `suspended_task`. This argument should be a function that is being executed
/// on Tokio's blocking pool.
/// `followed_by`: the next function to invoke if no errors have been encountered.
///
/// # Returns
/// Returns `None` if `runtime_requests` has been dropped. This indicates that the agent should
/// shutdown. Returns `Some` if the runtime was not dropped.
async fn suspend<'l, F, C, A>(
    agent_environment: &'l mut GuestEnvironment<A>,
    agent_context: &Box<dyn AgentContext + Send>,
    runtime_requests: &mut mpsc::Receiver<GuestRuntimeRequest>,
    task_scheduler: &mut TaskScheduler,
    guest_context: &C,
    suspended_task: F,
) -> Result<ControlFlow<()>, AgentTaskError>
where
    F: Future<Output = Result<Vec<u8>, AgentTaskError>>,
    A: GuestAgentVTable,
    C: GuestRuntimeContext,
{
    pin!(suspended_task);

    debug!("Agent runtime suspended");

    let result = loop {
        let event: SuspendedRuntimeEvent = select! {
            biased;
            request = runtime_requests.recv() => SuspendedRuntimeEvent::Request(request),
            suspend_result = (&mut suspended_task) => SuspendedRuntimeEvent::SuspendComplete(suspend_result),
        };

        debug!(event = ?event, "Suspended runtime event received");

        match event {
            SuspendedRuntimeEvent::Request(Some(request)) => {
                trace!(request = ?request, "Agent runtime received a request");
                match request {
                    GuestRuntimeRequest::OpenLane { uri, spec } => {
                        match open_lane(spec, uri.as_str(), agent_context, agent_environment).await
                        {
                            Ok(()) => continue,
                            Err(OpenLaneError::AgentRuntime(e)) => {
                                break Err(AgentTaskError::UserCodeError(Box::new(e)))
                            }
                            Err(OpenLaneError::FrameIo(error)) => {
                                break Err(AgentTaskError::BadFrame { lane: uri, error })
                            }
                        }
                    }
                    GuestRuntimeRequest::ScheduleTask { id, schedule } => {
                        trace!(id = ?id, schedule = ?schedule, "Scheduling task");
                        task_scheduler.push_task(id, schedule);
                    }
                    GuestRuntimeRequest::CancelTask { id } => task_scheduler.cancel_task(id),
                }
            }
            SuspendedRuntimeEvent::Request(None) => {
                // Agent context has been dropped.
                debug!("Agent context has been dropped. Shutting down agent");
                break Ok(ControlFlow::Break(()));
            }
            SuspendedRuntimeEvent::SuspendComplete(Ok(responses)) => {
                break forward_lane_responses(
                    guest_context,
                    &agent_environment.guest_agent,
                    BytesMut::from_iter(responses),
                    &mut agent_environment.lane_writers,
                )
                .await
            }
            SuspendedRuntimeEvent::SuspendComplete(Err(e)) => break Err(e),
        }
    };

    debug!(result = ?result, "Agent runtime suspend complete");
    result
}

/// Requests from the guest runtime to the host.
#[derive(Debug)]
pub enum GuestRuntimeRequest {
    /// Open a new lane
    OpenLane {
        /// The URI of the lane
        uri: Text,
        /// The specification of the lane.
        spec: LaneSpec,
    },
    /// Schedule a new task
    ScheduleTask {
        /// The ID of the task
        id: Uuid,
        /// The execution schedule of the task
        schedule: ScheduleDef,
    },
    /// Cancel a previously scheduled task
    CancelTask {
        /// The ID of the task
        id: Uuid,
    },
}

async fn forward_lane_responses<C, A>(
    ctx: &C,
    vtable: &A,
    mut data: BytesMut,
    lane_writers: &mut HashMap<i32, ByteWriter>,
) -> Result<ControlFlow<()>, AgentTaskError>
where
    C: GuestRuntimeContext,
    A: GuestAgentVTable,
{
    let mut decoder = LaneResponseDecoder::default();

    loop {
        match decoder.decode(&mut data) {
            Ok(Some(LaneResponseElement::Feed)) => {
                data = BytesMut::from_iter(vtable.flush_state().await?.as_slice());
            }
            Ok(Some(LaneResponseElement::Response { lane_id, data })) => {
                match lane_writers.get_mut(&lane_id) {
                    Some(writer) => {
                        if writer.write_all(data.as_ref()).await.is_err() {
                            // if the writer has closed then it indicates that the runtime has
                            // shutdown and it is safe to sink the error.
                            return Ok(ControlFlow::Break(()));
                        }
                    }
                    None => {
                        return Err(ctx.fatal_error(format!("Missing lane writer: {}", lane_id)))
                    }
                }
            }
            Ok(None) => {
                break Ok(ControlFlow::Continue(()));
            }
            Err(_) => return Err(ctx.fatal_error("Agent received invalid bytes from runtime")),
        }
    }
}

struct LaneReader {
    idx: i32,
    codec: LaneReaderCodec,
}

impl LaneReader {
    fn value(idx: i32, reader: ByteReader) -> LaneReader {
        LaneReader {
            idx,
            codec: LaneReaderCodec::value(reader),
        }
    }

    fn map(idx: i32, reader: ByteReader) -> LaneReader {
        LaneReader {
            idx,
            codec: LaneReaderCodec::map(reader),
        }
    }
}

impl Stream for LaneReader {
    type Item = (i32, Result<LaneRequest<BytesMut>, FrameIoError>);

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let LaneReader { idx, codec } = self.get_mut();
        Pin::new(codec)
            .poll_next(cx)
            .map(|r| r.map(|result| (*idx, result)))
    }
}
