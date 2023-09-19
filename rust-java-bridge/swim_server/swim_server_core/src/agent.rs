use std::collections::HashMap;
use std::future::Future;
use std::mem::size_of;
use std::ops::{ControlFlow, Deref};
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use bytes::BytesMut;
use futures::{pin_mut, StreamExt};
use futures::{Sink, Stream};
use futures_util::future::BoxFuture;
use futures_util::stream::{unfold, FuturesUnordered, SelectAll};
use futures_util::{FutureExt, SinkExt};
use jni::errors::Error as JavaError;
use jni::objects::{GlobalRef, JByteBuffer, JObject, JThrowable, JValue};
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
use tokio::select;
use tokio::sync::mpsc;
use tokio::task::{JoinError, JoinHandle};
use tokio_util::codec::{Decoder, Encoder, FramedRead, FramedWrite};
use tracing::{debug, error, info, span, trace, Level};
use tracing_futures::Instrument;
use uuid::Uuid;

use jvm_sys::env::{
    ByteBufferGuard, GlobalRefByteBuffer, IsTypeOfExceptionHandler, JObjectFromByteBuffer, JavaEnv,
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
                JavaAgentRef::new(obj_ref, vtable.clone())
            })
        }
    }
}

#[derive(Debug, Clone)]
pub struct JavaAgentRef {
    agent_obj: GlobalRef,
    vtable: Arc<JavaAgentVTable>,
}

impl JavaAgentRef {
    fn new(agent_obj: GlobalRef, vtable: Arc<JavaAgentVTable>) -> JavaAgentRef {
        JavaAgentRef { agent_obj, vtable }
    }

    fn did_start(&self, env: &JavaEnv) -> Result<(), AgentTaskError> {
        trace!("Invoking agent did_start method");

        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.did_start(&scope, agent_obj.as_obj()))
    }

    fn did_stop(&self, env: &JavaEnv) -> Result<(), AgentTaskError> {
        trace!("Invoking agent did_stop method");
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.did_stop(&scope, agent_obj.as_obj()))
    }

    fn dispatch<T>(&self, env: &JavaEnv, lane_id: i32, buffer: T) -> Result<Vec<u8>, AgentTaskError>
    where
        T: JObjectFromByteBuffer,
    {
        trace!("Dispatching event to agent");
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| {
            let obj = buffer.as_byte_buffer();
            vtable.dispatch(&scope, agent_obj.as_obj(), lane_id, obj)
        })
    }

    fn sync(&self, env: &JavaEnv, lane_id: i32, remote: Uuid) -> Result<Vec<u8>, AgentTaskError> {
        trace!(lane_id, remote = %remote, "Dispatching sync for lane");
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.sync(&scope, agent_obj.as_obj(), lane_id, remote))
    }

    fn init(&self, env: &JavaEnv, lane_id: i32, mut msg: BytesMut) {
        trace!(lane_id, "Initialising lane");
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(&mut msg) };
            vtable.init(&scope, agent_obj.as_obj(), lane_id, buffer)
        })
    }

    fn flush_state(&self, env: &JavaEnv) -> Result<Vec<u8>, AgentTaskError> {
        trace!("Flushing agent state");
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.flush_state(&scope, agent_obj.as_obj()))
    }
}

#[derive(Debug)]
pub struct JavaAgentVTable {
    did_start: InitialisedJavaObjectMethod,
    did_stop: InitialisedJavaObjectMethod,
    dispatch: InitialisedJavaObjectMethod,
    sync: InitialisedJavaObjectMethod,
    init: InitialisedJavaObjectMethod,
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
        "(ILjava/nio/ByteBuffer;)[B",
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

    fn initialise(env: &JavaEnv) -> JavaAgentVTable {
        JavaAgentVTable {
            did_start: env.initialise(Self::DID_START),
            did_stop: env.initialise(Self::DID_STOP),
            dispatch: env.initialise(Self::DISPATCH),
            sync: env.initialise(Self::SYNC),
            init: env.initialise(Self::INIT),
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
    ) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable {
            dispatch, handler, ..
        } = self;
        dispatch.l().array::<ByteArray>().invoke(
            handler,
            scope,
            agent_obj,
            &[lane_id.into(), msg.into()],
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

    fn init(&self, scope: &Scope, agent_obj: JObject, lane_id: jint, msg: ByteBufferGuard) {
        let JavaAgentVTable { init, .. } = self;
        scope.invoke(init.v(), agent_obj, &[lane_id.into(), msg.into()])
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
            Handle::current(),
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
                        ffi_context.clone(),
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
                        ffi_context.clone(),
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
        route,
        route_params,
        config,
        lane_identifiers,
        java_agent,
        lane_readers,
        lane_writers,
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
    fn initialize<'l, S>(
        &'l self,
        context: FfiContext,
        stream: S,
    ) -> BoxFuture<'l, Result<(), FrameIoError>>
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
    fn initialize<'l, S>(
        &'l self,
        context: FfiContext,
        stream: S,
    ) -> BoxFuture<'l, Result<(), FrameIoError>>
    where
        S: Stream<Item = Result<BytesMut, FrameIoError>> + Send + 'l,
    {
        let JavaValueLikeLaneInitializer { agent_obj, lane_id } = self;
        Box::pin(async move {
            match try_last(stream).await? {
                Some(body) => {
                    agent_obj.init(&context.env, *lane_id, body);
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
    fn initialize<'l, S>(
        &'l self,
        context: FfiContext,
        mut stream: S,
    ) -> BoxFuture<'l, Result<(), FrameIoError>>
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
                            agent_obj.init(&context.env, *lane_id, buf);
                            buf = BytesMut::new();
                        }
                    }
                    Err(e) => return Err(e),
                }
            }

            if !buf.is_empty() {
                agent_obj.init(&context.env, *lane_id, buf);
            }

            Ok(())
        })
    }
}

async fn run_lane_initializer<D>(
    context: FfiContext,
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
    match initializer.initialize(context, stream).await {
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

#[derive(Clone)]
struct LaneIdentifier {
    id: i32,
    str: Text,
}

enum AgentTaskState {
    Running,
    Suspended(JoinHandle<Result<Vec<u8>, AgentTaskError>>),
}

#[derive(Debug)]
enum RuntimeEvent {
    Request { id: i32, request: TaggedLaneRequest },
    RequestError { id: i32, error: FrameIoError },
}

enum SuspendedRuntimeEvent {
    Request(Option<AgentRuntimeRequest>),
    SuspendComplete(Result<Result<Vec<u8>, AgentTaskError>, JoinError>),
}

struct FfiAgentTask {
    ffi_context: FfiContext,
    route: RouteUri,
    route_params: HashMap<String, String>,
    config: AgentConfig,
    lane_identifiers: HashMap<i32, Text>,
    java_agent: JavaAgentRef,
    lane_readers: SelectAll<LaneReader>,
    lane_writers: HashMap<i32, ByteWriter>,
    runtime_requests: mpsc::Receiver<AgentRuntimeRequest>,
    agent_context: Box<dyn AgentContext + Send>,
}

impl FfiAgentTask {
    async fn run(self) -> Result<(), AgentTaskError> {
        let FfiAgentTask {
            ffi_context,
            route,
            route_params,
            config,
            lane_identifiers,
            java_agent,
            mut lane_readers,
            mut lane_writers,
            mut runtime_requests,
            agent_context,
        } = self;

        info!("Running agent");

        let env = ffi_context.env.clone();
        java_agent.did_start(&env)?;

        let mut buf = BytesMut::with_capacity(128);
        let byte_buffer = {
            let (addr, len) = { (buf.as_mut_ptr(), buf.capacity()) };
            unsafe { env.with_env(|scope| scope.new_direct_byte_buffer_global(addr, len)) }
        };

        let mut state = AgentTaskState::Running;

        loop {
            match state {
                AgentTaskState::Running => {
                    let event: Option<RuntimeEvent> = select! {
                        message = lane_readers.next() => message.map(|(id, request)| {
                            match request {
                                Ok(request) => RuntimeEvent::Request { id, request },
                                Err(error) => RuntimeEvent::RequestError { id, error }
                            }
                        }),
                    };

                    debug!(event = ?event, "FFI agent task received runtime event");

                    match event {
                        Some(RuntimeEvent::Request { id, request }) => {
                            let bb = byte_buffer.clone();
                            state = match dispatch_request(
                                ffi_context.clone(),
                                java_agent.clone(),
                                id,
                                request,
                                &mut buf,
                                bb,
                            ) {
                                Some(handle) => {
                                    debug!("Suspending agent runtime");
                                    AgentTaskState::Suspended(handle)
                                }
                                None => AgentTaskState::Running,
                            }
                        }
                        Some(RuntimeEvent::RequestError { id, error }) => {
                            let lane = lane_identifiers.get(&id).cloned().expect("Missing lane");
                            return Err(AgentTaskError::BadFrame { lane, error });
                        }
                        None => break,
                    }
                }
                AgentTaskState::Suspended(mut handle) => {
                    let event: SuspendedRuntimeEvent = select! {
                        suspend_result = (&mut handle) => SuspendedRuntimeEvent::SuspendComplete(suspend_result),
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
                                    let mut lane_conf =
                                        config.default_lane_config.unwrap_or_default();
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
                                        lane_readers.push(reader);
                                        lane_writers.insert(lane_idx, tx);
                                    } else {
                                        let initialised = if lane_kind_repr.map_like() {
                                            run_lane_initializer(
                                                ffi_context.clone(),
                                                JavaMapLikeLaneInitializer::new(
                                                    java_agent.clone(),
                                                    spec.lane_idx,
                                                ),
                                                lane_kind_repr,
                                                (tx, rx),
                                                WithLengthBytesCodec::default(),
                                                lane_idx,
                                            )
                                            .await
                                            .map_err(
                                                |e| AgentTaskError::UserCodeError(Box::new(e)),
                                            )?
                                        } else {
                                            run_lane_initializer(
                                                ffi_context.clone(),
                                                JavaValueLikeLaneInitializer::new(
                                                    java_agent.clone(),
                                                    spec.lane_idx,
                                                ),
                                                lane_kind_repr,
                                                (tx, rx),
                                                WithLengthBytesCodec::default(),
                                                lane_idx,
                                            )
                                            .await
                                            .map_err(
                                                |e| AgentTaskError::UserCodeError(Box::new(e)),
                                            )?
                                        };

                                        let InitializedLane {
                                            kind,
                                            io: (tx, rx),
                                            id,
                                        } = initialised;
                                        lane_readers.push(LaneReader::value(id, rx));
                                        lane_writers.insert(id, tx);
                                    }
                                }
                            }
                            state = AgentTaskState::Suspended(handle);
                        }
                        SuspendedRuntimeEvent::Request(None) => {
                            // Java agent context has been GC'd.
                            debug!("Java agent context has been garbage collected. Shutting down agent");
                            return Ok(());
                        }
                        SuspendedRuntimeEvent::SuspendComplete(Ok(Ok(returned_data))) => {
                            trace!("Handling lane response");
                            forward_lane_responses(
                                &ffi_context,
                                &java_agent,
                                BytesMut::from_iter(returned_data),
                                &mut lane_writers,
                            )
                            .await?;
                            debug!("Resuming agent runtime");
                            state = AgentTaskState::Running;
                        }
                        SuspendedRuntimeEvent::SuspendComplete(Ok(Err(e))) => return Err(e),
                        SuspendedRuntimeEvent::SuspendComplete(Err(e)) => {
                            error!(error = ?e, "Suspended JNI call join error");
                            env.fatal_error(e)
                        }
                    }
                }
            }
        }

        java_agent.did_stop(&env)?;

        Ok(())
    }
}

fn dispatch_request(
    context: FfiContext,
    agent_ref: JavaAgentRef,
    lane_id: i32,
    request: TaggedLaneRequest,
    buffer_content: &mut BytesMut,
    byte_buffer: GlobalRefByteBuffer,
) -> Option<JoinHandle<Result<Vec<u8>, AgentTaskError>>> {
    let TaggedLaneRequest { request, .. } = request;
    let runtime_handle = Handle::current();
    let env = context.env.clone();

    match request {
        LaneRequest::Command(mut msg) => {
            trace!("Received a command request");

            // todo: implement an EWMA to track the message sizes and reallocate the buffer if
            //  there is frequent access into the slow path.

            let handle = if msg.len() > buffer_content.capacity() {
                // Slow path where the message is too big to fit into the buffer. It's faster to
                // allocate a new buffer and still perform the JNI call in one hop as opposed to
                // executing multiple calls, performing incremental decoding of the message and
                // allocating temporary buffers.

                runtime_handle.spawn_blocking(move || {
                    // The message **must** live for the duration of the JNI call.
                    //
                    // This is different to below where the message is copied into 'buffer_content'
                    // as the buffer lives for the duration of the JNI call.
                    let mut msg = msg;

                    let temp_buffer =
                        env.with_env(|scope| scope.new_direct_byte_buffer_exact(&mut msg));
                    let result = agent_ref.dispatch(&env, lane_id, temp_buffer);

                    // Free the local ref created when allocating the direct byte buffer. This
                    // should be performed within the dispatch local frame but it will require
                    // additional work to be moved there.
                    env.with_env(|scope| scope.delete_local_ref(temp_buffer.as_byte_buffer()));

                    result
                })
            } else {
                // Fast path where the message will fit into the existing direct byte buffer by
                // copying the bytes into it.

                buffer_content.clear();
                buffer_content.extend_from_slice(msg.as_ref());

                runtime_handle
                    .spawn_blocking(move || agent_ref.dispatch(&env, lane_id, byte_buffer))
            };
            Some(handle)
        }
        LaneRequest::Sync(remote_id) => {
            trace!("Received a sync request");
            let handle = runtime_handle
                .spawn_blocking(move || agent_ref.sync(&context.env, lane_id, remote_id));
            Some(handle)
        }
        LaneRequest::InitComplete => None,
    }
}

#[derive(Debug)]
pub enum AgentRuntimeRequest {
    OpenLane { uri: Text, spec: LaneSpec },
}

enum FfiAgentError {
    Java(JavaError),
    Decode(FrameIoError),
}

impl From<std::io::Error> for FfiAgentError {
    fn from(value: std::io::Error) -> Self {
        FfiAgentError::Decode(FrameIoError::Io(value))
    }
}

impl From<JavaError> for FfiAgentError {
    fn from(value: JavaError) -> Self {
        FfiAgentError::Java(value)
    }
}

impl From<FrameIoError> for FfiAgentError {
    fn from(value: FrameIoError) -> Self {
        FfiAgentError::Decode(value)
    }
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
                data = agent_ref.flush_state(&ctx.env).map(BytesMut::from_iter)?;
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

struct LaneWriter {
    idx: i32,
    writer: ByteWriter,
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

#[derive(Debug)]
pub enum LaneType {
    Value,
    Map,
}

#[derive(Debug)]
struct TaggedLaneRequest {
    lane_type: LaneType,
    request: LaneRequest<BytesMut>,
}

impl Stream for LaneReader {
    type Item = (i32, Result<TaggedLaneRequest, FrameIoError>);

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        let LaneReader { idx, codec } = self.get_mut();
        Pin::new(codec).poll_next(cx).map(|r| {
            r.map(|(lane_type, result)| {
                (
                    *idx,
                    result.map(|request| TaggedLaneRequest { lane_type, request }),
                )
            })
        })
    }
}
