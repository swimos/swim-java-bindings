#[cfg(test)]
mod tests;

use std::collections::HashMap;
use std::fmt::Debug;
use std::future::{ready, Future};
use std::mem::size_of;
use std::ops::ControlFlow;
use std::pin::Pin;
use std::task::{Context, Poll};
use std::time::Duration;

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
use tracing::{debug, info, span, trace, Level};
use tracing_futures::Instrument;

use interval_stream::{IntervalStream, ScheduleDef, StreamItem};

use crate::agent::foreign::{AgentFactory, AgentVTable, RuntimeContext};
use crate::agent::spec::{AgentSpec, LaneSpec};
use crate::codec::{LaneReaderCodec, LaneResponseDecoder, LaneResponseElement};

pub mod context;
pub mod foreign;
pub mod spec;

#[derive(Debug, Clone)]
pub struct FfiAgentDef<C, F> {
    ffi_context: C,
    spec: AgentSpec,
    agent_factory: F,
}

impl<C, F> FfiAgentDef<C, F> {
    pub fn new(ffi_context: C, spec: AgentSpec, agent_factory: F) -> FfiAgentDef<C, F> {
        FfiAgentDef {
            ffi_context,
            spec,
            agent_factory,
        }
    }
}

impl<C, F> Agent for FfiAgentDef<C, F>
where
    F: AgentFactory<Context = C>,
    C: RuntimeContext,
{
    fn run(
        &self,
        route: RouteUri,
        route_params: HashMap<String, String>,
        config: AgentConfig,
        context: Box<dyn AgentContext + Send>,
    ) -> BoxFuture<'static, AgentInitResult> {
        let FfiAgentDef {
            ffi_context,
            spec,
            agent_factory,
        } = self;

        // todo: channel size from config
        let (ffi_tx, ffi_rx) = mpsc::channel(8);

        let uri_string = route.to_string();
        let create_result =
            agent_factory.agent_for(ffi_context.clone(), uri_string.as_str(), ffi_tx);
        let ffi_context = ffi_context.clone();
        let spec = spec.clone();

        let task = async move {
            match create_result {
                Ok(agent) => initialize_agent(
                    ffi_context,
                    spec,
                    route,
                    route_params,
                    config,
                    context,
                    agent,
                    ffi_rx,
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
    agent_environment: &mut AgentEnvironment<A>,
) -> Result<(), OpenLaneError>
where
    A: AgentVTable,
{
    let AgentEnvironment {
        config,
        lane_identifiers,
        ffi_agent,
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
                MapLikeLaneNativeInitializer::new(ffi_agent, spec.lane_idx),
                (tx, rx),
                WithLengthBytesCodec::default(),
                lane_idx,
            )
            .await?;

            (LaneReader::map(id, rx), id, tx)
        } else {
            let InitializedLane { io: (tx, rx), id } = run_lane_initializer(
                ValueLikeLaneNativeInitializer::new(ffi_agent, spec.lane_idx),
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
    ffi_context: C,
    spec: AgentSpec,
    route: RouteUri,
    route_params: HashMap<String, String>,
    config: AgentConfig,
    agent_context: Box<dyn AgentContext + Send>,
    ffi_agent: A,
    runtime_requests: mpsc::Receiver<AgentRuntimeRequest>,
) -> Result<FfiAgentTask<A, C>, AgentInitError>
where
    A: AgentVTable,
{
    let AgentSpec { lane_specs, .. } = spec;

    let mut agent_environment = AgentEnvironment {
        _route: route,
        _route_params: route_params,
        config,
        lane_identifiers: HashMap::new(),
        ffi_agent,
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
                return Err(AgentInitError::LaneInitializationFailure(error))
            }
        }
    }

    debug!("Agent initialised");

    Ok(FfiAgentTask {
        ffi_context,
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

trait NativeItemInitializer {
    fn initialize<'s, S>(&'s mut self, stream: S) -> BoxFuture<Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 's;
}

struct ValueLikeLaneNativeInitializer<'a, A> {
    agent_obj: &'a mut A,
    lane_id: i32,
}

impl<'a, A> ValueLikeLaneNativeInitializer<'a, A> {
    pub fn new(agent_obj: &'a mut A, lane_id: i32) -> ValueLikeLaneNativeInitializer<'a, A> {
        ValueLikeLaneNativeInitializer { agent_obj, lane_id }
    }
}

impl<'a, A> NativeItemInitializer for ValueLikeLaneNativeInitializer<'a, A>
where
    A: AgentVTable,
{
    fn initialize<'s, S>(&'s mut self, stream: S) -> BoxFuture<Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 's,
    {
        let ValueLikeLaneNativeInitializer { agent_obj, lane_id } = self;
        Box::pin(async move {
            match try_last(stream).await? {
                Some(body) => {
                    agent_obj.init(*lane_id, body);
                    Ok(())
                }
                None => Ok(()),
            }
        })
    }
}

struct MapLikeLaneNativeInitializer<'a, A> {
    agent_obj: &'a mut A,
    lane_id: i32,
}

impl<'a, A> MapLikeLaneNativeInitializer<'a, A> {
    pub fn new(agent_obj: &'a mut A, lane_id: i32) -> MapLikeLaneNativeInitializer<'a, A> {
        MapLikeLaneNativeInitializer { agent_obj, lane_id }
    }
}

impl<'a, A> NativeItemInitializer for MapLikeLaneNativeInitializer<'a, A>
where
    A: AgentVTable,
{
    fn initialize<'s, S>(&'s mut self, stream: S) -> BoxFuture<Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 's,
    {
        let MapLikeLaneNativeInitializer { agent_obj, lane_id } = self;
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
    I: NativeItemInitializer,
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
                .map_err(FrameIoError::Io)
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
        event: StreamItem<TaskDef>,
    },
}

#[derive(Clone, Debug)]
struct TaskDef {
    id_lsb: i64,
    id_msb: i64,
}

struct AgentEnvironment<V> {
    _route: RouteUri,
    _route_params: HashMap<String, String>,
    config: AgentConfig,
    lane_identifiers: HashMap<i32, Text>,
    ffi_agent: V,
    lane_readers: SelectAll<LaneReader>,
    lane_writers: HashMap<i32, ByteWriter>,
}

struct FfiAgentTask<A, C> {
    ffi_context: C,
    agent_environment: AgentEnvironment<A>,
    runtime_requests: mpsc::Receiver<AgentRuntimeRequest>,
    agent_context: Box<dyn AgentContext + Send>,
}

impl<A, C> FfiAgentTask<A, C> {
    async fn run(self) -> Result<(), AgentTaskError>
    where
        A: AgentVTable,
        C: RuntimeContext,
    {
        let FfiAgentTask {
            ffi_context,
            mut agent_environment,
            mut runtime_requests,
            agent_context,
        } = self;

        info!("Running agent");

        agent_environment.ffi_agent.did_start().await?;

        let mut task_scheduler = IntervalStream::<TaskDef>::default();

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
                    task = task_scheduler.next() => task.map(|event| RuntimeEvent::ScheduledEvent {event})
                }
            };

            debug!(event = ?event, "FFI agent task received runtime event");

            match event {
                Some(RuntimeEvent::Request { id, request }) => {
                    let handle = match request {
                        LaneRequest::Command(msg) => {
                            if i32::try_from(msg.len()).is_err() {
                                // todo: the peer should be unlinked
                                unimplemented!("oversized messages");
                            }

                            trace!("Received a command request");
                            agent_environment.ffi_agent.dispatch(id, msg)
                        }
                        LaneRequest::Sync(remote_id) => {
                            trace!("Received a sync request");
                            agent_environment.ffi_agent.sync(id, remote_id)
                        }
                        LaneRequest::InitComplete => continue,
                    };

                    if let Some(ControlFlow::Break(())) = suspend(
                        &mut agent_environment,
                        &agent_context,
                        &mut runtime_requests,
                        &mut task_scheduler,
                        handle,
                        |agent_environment, suspend_result| async {
                            match suspend_result {
                                Ok(responses) => {
                                    trace!("Handling lane response");
                                    let r = forward_lane_responses(
                                        &ffi_context,
                                        &agent_environment.ffi_agent,
                                        BytesMut::from_iter(responses),
                                        &mut agent_environment.lane_writers,
                                    )
                                    .await;
                                    debug!("Resuming agent runtime");
                                    r
                                }

                                Err(e) => Err(e),
                            }
                        },
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
                    let task_def = event.item;
                    let handle = agent_environment
                        .ffi_agent
                        .run_task(task_def.id_msb, task_def.id_lsb);

                    suspend(
                        &mut agent_environment,
                        &agent_context,
                        &mut runtime_requests,
                        &mut task_scheduler,
                        handle,
                        |_, _| ready(Ok(())),
                    )
                    .await?;
                }
            }
        }

        agent_environment.ffi_agent.did_stop().await?;

        Ok(())
    }
}

enum SuspendedRuntimeEvent<O> {
    Request(Option<AgentRuntimeRequest>),
    SuspendComplete(O),
}

async fn suspend<'l, F, O, H, HF, HR, A>(
    agent_environment: &'l mut AgentEnvironment<A>,
    agent_context: &Box<dyn AgentContext + Send>,
    runtime_requests: &mut mpsc::Receiver<AgentRuntimeRequest>,
    task_scheduler: &mut IntervalStream<TaskDef>,
    fut: F,
    followed_by: H,
) -> Result<Option<HR>, AgentTaskError>
where
    F: Future<Output = O>,
    H: FnOnce(&'l mut AgentEnvironment<A>, O) -> HF,
    HF: Future<Output = Result<HR, AgentTaskError>> + 'l,
    A: AgentVTable,
{
    pin!(fut);

    loop {
        let event: SuspendedRuntimeEvent<O> = select! {
            suspend_result = (&mut fut) => SuspendedRuntimeEvent::SuspendComplete(suspend_result),
            request = runtime_requests.recv() => SuspendedRuntimeEvent::Request(request),
        };

        match event {
            SuspendedRuntimeEvent::Request(Some(request)) => {
                trace!(request = ?request, "Agent runtime received a request");
                match request {
                    AgentRuntimeRequest::OpenLane { uri, spec } => {
                        match open_lane(spec, uri.as_str(), agent_context, agent_environment).await
                        {
                            Ok(()) => continue,
                            Err(OpenLaneError::AgentRuntime(e)) => {
                                return Err(AgentTaskError::UserCodeError(Box::new(e)))
                            }
                            Err(OpenLaneError::FrameIo(error)) => {
                                return Err(AgentTaskError::BadFrame { lane: uri, error })
                            }
                        }
                    }
                    AgentRuntimeRequest::ScheduleTask {
                        id_lsb,
                        id_msb,
                        interval,
                    } => task_scheduler.push(
                        ScheduleDef::Infinite { interval },
                        TaskDef { id_lsb, id_msb },
                    ),
                }
            }
            SuspendedRuntimeEvent::Request(None) => {
                // Agent context has been dropped.
                debug!("Agent context has been dropped. Shutting down agent");
                return Ok(None);
            }
            SuspendedRuntimeEvent::SuspendComplete(output) => {
                return followed_by(agent_environment, output).await.map(Some)
            }
        }
    }
}

#[derive(Debug)]
pub enum AgentRuntimeRequest {
    OpenLane {
        uri: Text,
        spec: LaneSpec,
    },
    ScheduleTask {
        id_lsb: i64,
        id_msb: i64,
        interval: Duration,
    },
}

async fn forward_lane_responses<C, A>(
    ctx: &C,
    vtable: &A,
    mut data: BytesMut,
    lane_writers: &mut HashMap<i32, ByteWriter>,
) -> Result<ControlFlow<()>, AgentTaskError>
where
    C: RuntimeContext,
    A: AgentVTable,
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
            Ok(None) => break Ok(ControlFlow::Continue(())),
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
