use std::collections::HashMap;
use std::fmt::Debug;
use std::future::{ready, Future};
use std::mem::size_of;
use std::ops::{ControlFlow, Deref};
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};
use std::time::Duration;

use bytes::BytesMut;
use futures::Stream;
use futures::{pin_mut, StreamExt};
use futures_util::future::BoxFuture;
use futures_util::stream::{unfold, FuturesUnordered, SelectAll};
use futures_util::{FutureExt, SinkExt};
use jni::objects::{GlobalRef, JObject, JThrowable, JValue};
use jni::sys::{jint, jobject};
use swim_api::agent::{Agent, AgentConfig, AgentContext, AgentInitResult};
use swim_api::error::{AgentInitError, AgentTaskError, FrameIoError};
use swim_api::meta::lane::LaneKind;
use swim_api::protocol::agent::{
    LaneRequest, StoreInitMessage, StoreInitMessageDecoder, StoreInitialized, StoreInitializedCodec,
};
use swim_api::protocol::WithLengthBytesCodec;
use swim_form::structural::read::ReadError;
use swim_model::Text;
use swim_utilities::future::try_last;
use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
use swim_utilities::routing::route_uri::RouteUri;
use tokio::io::AsyncWriteExt;
use tokio::runtime::Handle;
use tokio::sync::mpsc;
use tokio::task::JoinError;
use tokio::{pin, select};
use tokio_util::codec::{Decoder, FramedRead, FramedWrite};
use tracing::{debug, error, info, span, trace, Level};
use tracing_futures::Instrument;
use uuid::Uuid;

use interval_stream::{IntervalStream, ScheduleDef, StreamItem};
use jvm_sys::env::{
    BufPtr, ByteBufferGuard, IsTypeOfExceptionHandler, JObjectFromByteBuffer, JavaEnv,
    JavaExceptionHandler, NotTypeOfExceptionHandler, Scope,
};
use jvm_sys::method::{
    ByteArray, InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};

use crate::codec::{LaneReaderCodec, LaneResponseDecoder, LaneResponseElement};
use crate::java_context::JavaAgentContext;
use crate::spec::{AgentSpec, LaneKindRepr, LaneSpec};
use crate::FfiContext;

#[derive(Debug)]
struct ExceptionHandler {
    user: NotTypeOfExceptionHandler,
    deserialization: IsTypeOfExceptionHandler,
}

impl JavaExceptionHandler for ExceptionHandler {
    type Err = AgentTaskError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        let ExceptionHandler {
            user,
            deserialization,
        } = self;

        debug!("Inspecting error");

        match deserialization.inspect(scope, throwable) {
            Some(err) => {
                debug!("Detected deserialization erorr");
                Some(AgentTaskError::DeserializationFailed(ReadError::Message(
                    err.to_string().into(),
                )))
            }
            None => user.inspect(scope, throwable).map(|err| {
                debug!("Detected user code error");
                AgentTaskError::UserCodeError(Box::new(err))
            }),
        }
    }
}

#[derive(Debug, Clone)]
pub struct AgentFactory {
    new_agent_method: InitialisedJavaObjectMethod,
    factory: GlobalRef,
    vtable: Arc<JavaAgentVTable>,
}

impl AgentFactory {
    const NEW_AGENT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/AbstractSwimServerBuilder",
        "agentFor",
        "(Ljava/lang/String;J)Lai/swim/server/agent/AgentView;",
    );

    pub fn new(env: &JavaEnv, factory: GlobalRef) -> AgentFactory {
        AgentFactory {
            new_agent_method: env.initialise(Self::NEW_AGENT),
            factory,
            vtable: Arc::new(JavaAgentVTable::initialise(env)),
        }
    }

    pub fn agent_for(
        &self,
        env: &JavaEnv,
        uri: impl AsRef<str>,
        ctx: *mut JavaAgentContext,
    ) -> JavaAgentRef {
        let node_uri = uri.as_ref();
        debug!(node_uri, "Attempting to create a new Java agent");

        let AgentFactory {
            new_agent_method,
            factory,
            vtable,
        } = self;
        unsafe {
            env.with_env(|scope| {
                let java_uri = scope.new_string(node_uri);
                let obj_ref = scope.invoke(
                    new_agent_method.l().global_ref(),
                    factory.as_obj(),
                    &[
                        JValue::Object(java_uri.deref().clone()),
                        JValue::Object(JObject::from_raw(ctx as u64 as jobject)),
                    ],
                );
                JavaAgentRef::new(env.clone(), obj_ref, vtable.clone())
            })
        }
    }
}

#[derive(Debug, Clone)]
pub struct JavaAgentRef {
    env: JavaEnv,
    agent_obj: GlobalRef,
    vtable: Arc<JavaAgentVTable>,
}

impl JavaAgentRef {
    fn new(env: JavaEnv, agent_obj: GlobalRef, vtable: Arc<JavaAgentVTable>) -> JavaAgentRef {
        JavaAgentRef {
            env,
            agent_obj,
            vtable,
        }
    }

    fn did_start(&self) -> Result<(), AgentTaskError> {
        trace!("Invoking agent did_start method");

        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| vtable.did_start(&scope, agent_obj.as_obj()))
    }

    fn did_stop(&self) -> Result<(), AgentTaskError> {
        trace!("Invoking agent did_stop method");
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| vtable.did_stop(&scope, agent_obj.as_obj()))
    }

    fn dispatch<T>(&self, lane_id: i32, buffer: &T, len: i32) -> Result<Vec<u8>, AgentTaskError>
    where
        T: JObjectFromByteBuffer,
    {
        trace!("Dispatching event to agent");
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| {
            let obj = buffer.as_byte_buffer();
            vtable.dispatch(&scope, agent_obj.as_obj(), lane_id, obj, len)
        })
    }

    fn sync(&self, lane_id: i32, remote: Uuid) -> Result<Vec<u8>, AgentTaskError> {
        trace!(lane_id, remote = %remote, "Dispatching sync for lane");
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| vtable.sync(&scope, agent_obj.as_obj(), lane_id, remote))
    }

    fn init(&self, lane_id: i32, mut msg: BytesMut) {
        trace!(lane_id, "Initialising lane");
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| {
            let buffer = scope.new_direct_byte_buffer_exact(&mut msg);
            vtable.init(&scope, agent_obj.as_obj(), lane_id, buffer)
        })
    }

    fn flush_state(&self) -> Result<Vec<u8>, AgentTaskError> {
        trace!("Flushing agent state");
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| vtable.flush_state(&scope, agent_obj.as_obj()))
    }

    fn run_task(&self, id_msb: i64, id_lsb: i64) {
        trace!(id_msb, id_lsb, "Running task");
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
        } = self;
        env.with_env(|scope| vtable.run_task(&scope, agent_obj.as_obj(), id_msb, id_lsb))
    }
}

#[derive(Debug)]
pub struct JavaAgentVTable {
    did_start: InitialisedJavaObjectMethod,
    did_stop: InitialisedJavaObjectMethod,
    dispatch: InitialisedJavaObjectMethod,
    sync: InitialisedJavaObjectMethod,
    init: InitialisedJavaObjectMethod,
    run_task: InitialisedJavaObjectMethod,
    flush_state: InitialisedJavaObjectMethod,
    handler: ExceptionHandler,
}

impl JavaAgentVTable {
    const DID_START: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "didStart", "()V");
    const DID_STOP: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "didStop", "()V");
    const DISPATCH: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentView",
        "dispatch",
        "(ILjava/nio/ByteBuffer;I)[B",
    );
    const SYNC: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "sync", "(IJJ)[B");
    const INIT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentView",
        "init",
        "(ILjava/nio/ByteBuffer;)V",
    );
    const FLUSH_STATE: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "flushState", "()[B");
    const RUN_TASK: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "runTask", "(II)V");

    fn initialise(env: &JavaEnv) -> JavaAgentVTable {
        JavaAgentVTable {
            did_start: env.initialise(Self::DID_START),
            did_stop: env.initialise(Self::DID_STOP),
            dispatch: env.initialise(Self::DISPATCH),
            sync: env.initialise(Self::SYNC),
            init: env.initialise(Self::INIT),
            run_task: env.initialise(Self::RUN_TASK),
            flush_state: env.initialise(Self::FLUSH_STATE),
            handler: ExceptionHandler {
                user: NotTypeOfExceptionHandler::new(env, "ai/swim/server/agent/AgentException"),
                deserialization: IsTypeOfExceptionHandler::new(
                    env,
                    "ai/swim/server/agent/AgentException",
                ),
            },
        }
    }

    fn did_start(&self, scope: &Scope, agent_obj: JObject) -> Result<(), AgentTaskError> {
        let JavaAgentVTable {
            did_start, handler, ..
        } = self;
        did_start.v().invoke(handler, scope, agent_obj, &[])
    }

    fn did_stop(&self, scope: &Scope, agent_obj: JObject) -> Result<(), AgentTaskError> {
        let JavaAgentVTable {
            did_stop, handler, ..
        } = self;
        did_stop.v().invoke(handler, scope, agent_obj, &[])
    }

    fn dispatch(
        &self,
        scope: &Scope,
        agent_obj: JObject,
        lane_id: jint,
        msg: JObject,
        len: i32,
    ) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable {
            dispatch, handler, ..
        } = self;
        dispatch.l().array::<ByteArray>().invoke(
            handler,
            scope,
            agent_obj,
            &[lane_id.into(), msg.into(), len.into()],
        )
    }

    fn sync(
        &self,
        scope: &Scope,
        agent_obj: JObject,
        lane_id: jint,
        remote: Uuid,
    ) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable { sync, handler, .. } = self;
        let try_into = |num: u64| -> Result<i64, AgentTaskError> {
            match num.try_into() {
                Ok(n) => Ok(n),
                Err(_) => {
                    return Err(AgentTaskError::DeserializationFailed(
                        ReadError::NumberOutOfRange,
                    ))
                }
            }
        };

        let (msb, lsb) = remote.as_u64_pair();
        let msb = try_into(msb)?;
        let lsb = try_into(lsb)?;

        sync.l().array::<ByteArray>().invoke(
            handler,
            scope,
            agent_obj,
            &[lane_id.into(), msb.into(), lsb.into()],
        )
    }

    fn flush_state(&self, scope: &Scope, agent_obj: JObject) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable {
            flush_state,
            handler,
            ..
        } = self;
        flush_state
            .l()
            .array::<ByteArray>()
            .invoke(handler, scope, agent_obj, &[])
    }

    fn init<B>(&self, scope: &Scope, agent_obj: JObject, lane_id: jint, msg: ByteBufferGuard<B>)
    where
        B: BufPtr,
    {
        let JavaAgentVTable { init, .. } = self;
        scope.invoke(init.v(), agent_obj, &[lane_id.into(), msg.into()])
    }

    fn run_task(&self, scope: &Scope, agent_obj: JObject, id_msb: i64, id_lsb: i64) {
        let JavaAgentVTable { run_task, .. } = self;
        scope.invoke(run_task.v(), agent_obj, &[id_msb.into(), id_lsb.into()])
    }
}

#[derive(Debug, Clone)]
pub struct FfiAgentDef {
    ffi_context: FfiContext,
    spec: AgentSpec,
    agent_factory: AgentFactory,
}

impl FfiAgentDef {
    pub fn new(
        ffi_context: FfiContext,
        spec: AgentSpec,
        agent_factory: AgentFactory,
    ) -> FfiAgentDef {
        FfiAgentDef {
            ffi_context,
            spec,
            agent_factory,
        }
    }
}

impl Agent for FfiAgentDef {
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

        let (ffi_tx, ffi_rx) = mpsc::channel(8);
        let java_agent_ctx = Box::leak(Box::new(JavaAgentContext::new(
            ffi_context.env.clone(),
            ffi_tx,
        )));

        let uri_string = route.to_string();

        let java_agent =
            agent_factory.agent_for(&ffi_context.env, uri_string.as_str(), java_agent_ctx);
        let ffi_context = ffi_context.clone();
        let spec = spec.clone();

        let task = async move {
            let init_result = initialize_agent(
                ffi_context,
                spec,
                route,
                route_params,
                config,
                context,
                java_agent,
                ffi_rx,
            )
            .await;

            match init_result {
                Ok(task) => {
                    info!("Initialized agent");
                    Ok(task.run().boxed())
                }
                Err(e) => Err(e),
            }
        };

        task.instrument(span!(Level::DEBUG, "Agent task", uri_string))
            .boxed()
    }
}

async fn initialize_agent(
    ffi_context: FfiContext,
    spec: AgentSpec,
    route: RouteUri,
    route_params: HashMap<String, String>,
    config: AgentConfig,
    context: Box<dyn AgentContext + Send>,
    java_agent: JavaAgentRef,
    runtime_requests: mpsc::Receiver<AgentRuntimeRequest>,
) -> Result<FfiAgentTask, AgentInitError> {
    let default_lane_config = config.default_lane_config.unwrap_or_default();
    let AgentSpec { lane_specs, .. } = spec;
    let mut lane_identifiers = HashMap::new();
    let mut init_tasks = FuturesUnordered::default();
    let mut lane_readers = SelectAll::new();
    let mut lane_writers = HashMap::new();

    for (uri, spec) in lane_specs.into_iter() {
        let LaneSpec {
            is_transient,
            lane_idx,
            lane_kind_repr,
        } = spec;
        let kind: LaneKind = lane_kind_repr.into();
        debug!(uri, lane_kind = ?lane_kind_repr, is_transient, "Adding lane");

        let text_uri: Text = uri.into();
        let mut lane_conf = default_lane_config;
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
            if lane_kind_repr.map_like() {
                init_tasks.push(
                    run_lane_initializer(
                        JavaMapLikeLaneInitializer::new(java_agent.clone(), spec.lane_idx),
                        lane_kind_repr,
                        (tx, rx),
                        WithLengthBytesCodec::default(),
                        lane_idx,
                    )
                    .boxed(),
                );
            } else {
                init_tasks.push(
                    run_lane_initializer(
                        JavaValueLikeLaneInitializer::new(java_agent.clone(), spec.lane_idx),
                        lane_kind_repr,
                        (tx, rx),
                        WithLengthBytesCodec::default(),
                        lane_idx,
                    )
                    .boxed(),
                );
            }
        }

        lane_identifiers.insert(spec.lane_idx, text_uri);
    }

    while let Some(init_result) = init_tasks.next().await {
        match init_result {
            Ok(lane) => {
                let InitializedLane {
                    kind,
                    io: (tx, rx),
                    id,
                } = lane;

                let reader = if kind.map_like() {
                    LaneReader::map(id, rx)
                } else {
                    LaneReader::value(id, rx)
                };

                lane_readers.push(reader);
                lane_writers.insert(id, tx);
            }
            Err(e) => {
                error!(error = ?e, "Failed to initialise agent");
                return Err(AgentInitError::LaneInitializationFailure(e));
            }
        }
    }

    debug!("Agent initialised");

    Ok(FfiAgentTask {
        ffi_context,
        agent_context: context,
        agent_environment: AgentEnvironment {
            _route: route,
            _route_params: route_params,
            config,
            lane_identifiers,
            java_agent,
            lane_readers,
            lane_writers,
        },
        runtime_requests,
    })
}

struct InitializedLane {
    kind: LaneKindRepr,
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

trait JavaItemInitializer {
    fn initialize<'l, S>(&'l self, stream: S) -> BoxFuture<'l, Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 'l;
}

struct JavaValueLikeLaneInitializer {
    agent_obj: JavaAgentRef,
    lane_id: i32,
}

impl JavaValueLikeLaneInitializer {
    pub fn new(agent_obj: JavaAgentRef, lane_id: i32) -> JavaValueLikeLaneInitializer {
        JavaValueLikeLaneInitializer { agent_obj, lane_id }
    }
}

impl JavaItemInitializer for JavaValueLikeLaneInitializer {
    fn initialize<'l, S>(&'l self, stream: S) -> BoxFuture<'l, Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 'l,
    {
        let JavaValueLikeLaneInitializer { agent_obj, lane_id } = self;
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

struct JavaMapLikeLaneInitializer {
    agent_obj: JavaAgentRef,
    lane_id: i32,
}

impl JavaMapLikeLaneInitializer {
    pub fn new(agent_obj: JavaAgentRef, lane_id: i32) -> JavaMapLikeLaneInitializer {
        JavaMapLikeLaneInitializer { agent_obj, lane_id }
    }
}

impl JavaItemInitializer for JavaMapLikeLaneInitializer {
    fn initialize<'l, S>(&'l self, stream: S) -> BoxFuture<'l, Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 'l,
    {
        let JavaMapLikeLaneInitializer { agent_obj, lane_id } = self;
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

async fn run_lane_initializer<D>(
    initializer: impl JavaItemInitializer,
    kind: LaneKindRepr,
    io: (ByteWriter, ByteReader),
    decoder: D,
    id: i32,
) -> Result<InitializedLane, FrameIoError>
where
    D: Decoder<Item = BytesMut> + Send + 'static,
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
                .map(move |_| InitializedLane {
                    kind,
                    io: (tx, rx),
                    id,
                })
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

struct AgentEnvironment {
    _route: RouteUri,
    _route_params: HashMap<String, String>,
    config: AgentConfig,
    lane_identifiers: HashMap<i32, Text>,
    java_agent: JavaAgentRef,
    lane_readers: SelectAll<LaneReader>,
    lane_writers: HashMap<i32, ByteWriter>,
}

struct FfiAgentTask {
    ffi_context: FfiContext,
    agent_environment: AgentEnvironment,
    runtime_requests: mpsc::Receiver<AgentRuntimeRequest>,
    agent_context: Box<dyn AgentContext + Send>,
}

impl FfiAgentTask {
    async fn run(self) -> Result<(), AgentTaskError> {
        let FfiAgentTask {
            ffi_context,
            mut agent_environment,
            mut runtime_requests,
            agent_context,
        } = self;

        info!("Running agent");

        let env = ffi_context.env.clone();
        let java_agent = agent_environment.java_agent.clone();
        java_agent.did_start()?;

        let mut buf = BytesMut::with_capacity(128);
        let byte_buffer = {
            let (addr, len) = { (buf.as_mut_ptr(), buf.capacity()) };
            unsafe { env.with_env(|scope| scope.new_direct_byte_buffer_global(addr, len)) }
        };

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
                    let runtime_handle = Handle::current();
                    let agent_ref = java_agent.clone();

                    let join_handle = match request {
                        LaneRequest::Command(msg) => {
                            if i32::try_from(msg.len()).is_err() {
                                // todo: the peer should be unlinked
                                unimplemented!("oversized messages");
                            }

                            trace!("Received a command request");

                            // todo: implement an EWMA to track the message sizes and reallocate the
                            //  buffer if  there is frequent access into the slow path.

                            let env = env.clone();
                            let handle = if msg.len() > buf.capacity() {
                                // Slow path where the message is too big to fit into the buffer.
                                // It's faster to allocate a new buffer and still perform the JNI
                                // call in one hop as opposed to executing multiple calls,
                                // performing incremental decoding of the message and allocating
                                // temporary buffers.

                                runtime_handle.spawn_blocking(move || {
                                    // The message **must** live for the duration of the JNI call.
                                    //
                                    // This is different to below where the message is copied into
                                    // 'buffer_content' as the buffer lives for the duration of the
                                    // JNI call.
                                    let mut msg = msg;
                                    let msg_len = msg.len() as i32;

                                    let temp_buffer = env.with_env(|scope| {
                                        scope.new_direct_byte_buffer_exact(&mut msg)
                                    });
                                    let byte_buffer_ref = temp_buffer.as_byte_buffer();
                                    let result = agent_ref.dispatch(id, &temp_buffer, msg_len);

                                    // Free the local ref created when allocating the direct byte
                                    // buffer. This should be performed within the dispatch local
                                    // frame but it will require additional work to be moved there.
                                    env.with_env(|scope| scope.delete_local_ref(byte_buffer_ref));

                                    result
                                })
                            } else {
                                // Fast path where the message will fit into the existing direct
                                // byte buffer by copying the bytes into it.

                                buf.clear();
                                buf.extend_from_slice(msg.as_ref());

                                let byte_buffer = byte_buffer.clone();

                                runtime_handle.spawn_blocking(move || {
                                    agent_ref.dispatch(id, &byte_buffer, msg.len() as i32)
                                })
                            };
                            handle
                        }
                        LaneRequest::Sync(remote_id) => {
                            trace!("Received a sync request");
                            let handle = runtime_handle
                                .spawn_blocking(move || agent_ref.sync(id, remote_id));
                            handle
                        }
                        LaneRequest::InitComplete => continue,
                    };

                    if let Some(ControlFlow::Break(())) = suspend(
                        &env,
                        &mut agent_environment,
                        &agent_context,
                        &mut runtime_requests,
                        &mut task_scheduler,
                        join_handle,
                        |agent_environment, suspend_result| async {
                            match suspend_result {
                                Ok(responses) => {
                                    trace!("Handling lane response");
                                    let r = forward_lane_responses(
                                        &ffi_context,
                                        &java_agent,
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
                    let runtime_handle = Handle::current();
                    let java_agent = java_agent.clone();

                    let join_handle = runtime_handle.spawn_blocking(move || {
                        java_agent.run_task(task_def.id_msb, task_def.id_lsb)
                    });

                    suspend(
                        &env,
                        &mut agent_environment,
                        &agent_context,
                        &mut runtime_requests,
                        &mut task_scheduler,
                        join_handle,
                        |_, _| ready(Ok(())),
                    )
                    .await?;
                }
            }
        }

        java_agent.did_stop()?;

        Ok(())
    }
}

enum SuspendedRuntimeEvent<O> {
    Request(Option<AgentRuntimeRequest>),
    SuspendComplete(Result<O, JoinError>),
}

async fn suspend<'l, F, O, H, HF, HR>(
    env: &JavaEnv,
    agent_environment: &'l mut AgentEnvironment,
    agent_context: &Box<dyn AgentContext + Send>,
    runtime_requests: &mut mpsc::Receiver<AgentRuntimeRequest>,
    task_scheduler: &mut IntervalStream<TaskDef>,
    fut: F,
    followed_by: H,
) -> Result<Option<HR>, AgentTaskError>
where
    F: Future<Output = Result<O, JoinError>>,
    H: FnOnce(&'l mut AgentEnvironment, O) -> HF,
    HF: Future<Output = Result<HR, AgentTaskError>> + 'l,
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
                        let LaneSpec {
                            is_transient,
                            lane_idx,
                            lane_kind_repr,
                        } = spec;
                        let kind: LaneKind = lane_kind_repr.into();
                        let mut lane_conf = agent_environment
                            .config
                            .default_lane_config
                            .unwrap_or_default();
                        lane_conf.transient = is_transient;

                        let (tx, rx) = agent_context
                            .add_lane(uri.as_str(), kind, lane_conf)
                            .await
                            .map_err(|e| AgentTaskError::UserCodeError(Box::new(e)))?;

                        if is_transient {
                            let reader = if lane_kind_repr.map_like() {
                                LaneReader::map(lane_idx, rx)
                            } else {
                                LaneReader::value(lane_idx, rx)
                            };
                            agent_environment.lane_readers.push(reader);
                            agent_environment.lane_writers.insert(lane_idx, tx);
                        } else {
                            let initialised = if lane_kind_repr.map_like() {
                                run_lane_initializer(
                                    JavaMapLikeLaneInitializer::new(
                                        agent_environment.java_agent.clone(),
                                        spec.lane_idx,
                                    ),
                                    lane_kind_repr,
                                    (tx, rx),
                                    WithLengthBytesCodec::default(),
                                    lane_idx,
                                )
                                .await
                                .map_err(|e| AgentTaskError::UserCodeError(Box::new(e)))?
                            } else {
                                run_lane_initializer(
                                    JavaValueLikeLaneInitializer::new(
                                        agent_environment.java_agent.clone(),
                                        spec.lane_idx,
                                    ),
                                    lane_kind_repr,
                                    (tx, rx),
                                    WithLengthBytesCodec::default(),
                                    lane_idx,
                                )
                                .await
                                .map_err(|e| AgentTaskError::UserCodeError(Box::new(e)))?
                            };

                            let InitializedLane {
                                kind,
                                io: (tx, rx),
                                id,
                            } = initialised;

                            let reader = if kind.map_like() {
                                LaneReader::map(id, rx)
                            } else {
                                LaneReader::value(id, rx)
                            };

                            agent_environment.lane_readers.push(reader);
                            agent_environment.lane_writers.insert(id, tx);
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
                // Java agent context has been GC'd.
                debug!("Java agent context has been garbage collected. Shutting down agent");
                return Ok(None);
            }
            SuspendedRuntimeEvent::SuspendComplete(Ok(output)) => {
                return followed_by(agent_environment, output).await.map(Some)
            }
            SuspendedRuntimeEvent::SuspendComplete(Err(e)) => {
                error!(error = ?e, "Suspended JNI call join error");
                env.fatal_error(e)
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

async fn forward_lane_responses(
    ctx: &FfiContext,
    agent_ref: &JavaAgentRef,
    mut data: BytesMut,
    lane_writers: &mut HashMap<i32, ByteWriter>,
) -> Result<ControlFlow<()>, AgentTaskError> {
    let mut decoder = LaneResponseDecoder::default();

    loop {
        match decoder.decode(&mut data) {
            Ok(Some(LaneResponseElement::Feed)) => {
                data = agent_ref.flush_state().map(BytesMut::from_iter)?;
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
                    None => ctx.env.with_env(|scope| {
                        scope.fatal_error(format!("Missing lane writer: {}", lane_id))
                    }),
                }
            }
            Ok(None) => break Ok(ControlFlow::Continue(())),
            Err(_) => ctx.env.with_env(|scope| {
                scope.fatal_error("Agent received invalid bytes from Java runtime")
            }),
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
