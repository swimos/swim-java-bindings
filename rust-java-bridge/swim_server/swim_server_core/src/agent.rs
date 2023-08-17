use std::cell::RefCell;
use std::collections::HashMap;
use std::convert::Infallible;
use std::error::Error;
use std::future::Future;
use std::iter::StepBy;
use std::mem::{size_of, transmute};
use std::ops::{ControlFlow, Deref};
use std::panic::panic_any;
use std::pin::Pin;
use std::rc::Rc;
use std::sync::Arc;
use std::task::{Context, Poll};

use bytes::{Buf, BufMut, Bytes, BytesMut};
use futures::StreamExt;
use futures::{Sink, Stream};
use futures_util::future::BoxFuture;
use futures_util::stream::{unfold, BoxStream, FuturesUnordered, SelectAll};
use futures_util::{FutureExt, SinkExt};
use jni::errors::{Error as JavaError, JniError};
use jni::objects::{GlobalRef, JByteBuffer, JObject, JThrowable, JValue};
use jni::sys::{jint, jobject};
use jni::JNIEnv;
use swim_api::agent::{Agent, AgentConfig, AgentContext, AgentInitResult, LaneConfig, UplinkKind};
use swim_api::error::{AgentInitError, AgentTaskError, FrameIoError};
use swim_api::meta::lane::LaneKind;
use swim_api::protocol::agent::{
    LaneRequest, LaneRequestDecoder, LaneResponse, LaneResponseEncoder, StoreInitMessage,
    StoreInitMessageDecoder, StoreInitialized, StoreInitializedCodec, ValueLaneResponseEncoder,
};
use swim_api::protocol::map::{
    MapMessage, MapMessageDecoder, MapMessageEncoder, MapOperation, RawMapOperationDecoder,
};
use swim_api::protocol::{WithLenReconEncoder, WithLengthBytesCodec};
use swim_form::structural::read::ReadError;
use swim_model::Text;
use swim_recon::parser::{AsyncParseError, ParseError, RecognizerDecoder};
use swim_utilities::future::try_last;
use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
use swim_utilities::routing::route_uri::RouteUri;
use swim_utilities::trigger::trigger;
use tokio::io::AsyncWriteExt;
use tokio::select;
use tokio::sync::mpsc;
use tokio::task::spawn_blocking;
use tokio::time::Instant;
use tokio_util::codec::{BytesCodec, Decoder, Encoder, FramedRead, FramedWrite};
use tracing::{debug, error, info, trace};
use tracing_subscriber::fmt::init;
use uuid::Uuid;

use bytebridge::{ByteCodec, FromBytesError};
use jvm_sys::env::{
    ByteBufferGuard, IsTypeOfExceptionHandler, JavaEnv, JavaExceptionHandler, MethodResolver,
    Scope, StringError,
};
use jvm_sys::method::{
    ByteArray, InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};

use crate::codec::{LaneReaderCodec, LaneResponseDecoder, LaneResponseElement};
use crate::java_context::JavaAgentContext;
use crate::spec::{AgentSpec, LaneKindRepr, LaneSpec};
use crate::FfiContext;

#[derive(Debug)]
struct ExceptionHandler(IsTypeOfExceptionHandler);

impl JavaExceptionHandler for ExceptionHandler {
    type Err = AgentTaskError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        self.0
            .inspect(scope, throwable)
            .map(|e| AgentTaskError::UserCodeError(Box::new(e)))
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
        let AgentFactory {
            new_agent_method,
            factory,
            vtable,
        } = self;
        unsafe {
            env.with_env(|scope| {
                let java_uri = scope.new_string(uri.as_ref());
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
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.did_start(&scope, agent_obj.as_obj()))
    }

    fn did_stop(&self, env: &JavaEnv) -> Result<(), AgentTaskError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.did_stop(&scope, agent_obj.as_obj()))
    }

    fn dispatch(
        &self,
        env: &JavaEnv,
        lane_id: i32,
        mut msg: BytesMut,
    ) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(&mut msg) };
            vtable.dispatch(&scope, agent_obj.as_obj(), lane_id, buffer)
        })
    }

    fn sync(&self, env: &JavaEnv, lane_id: i32, remote: Uuid) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| vtable.sync(&scope, agent_obj.as_obj(), lane_id, remote))
    }

    fn init(&self, env: &JavaEnv, lane_id: i32, mut msg: BytesMut) {
        let JavaAgentRef { agent_obj, vtable } = self;
        env.with_env(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(&mut msg) };
            vtable.init(&scope, agent_obj.as_obj(), lane_id, buffer)
        })
    }

    fn flush_state(&self, env: &JavaEnv) -> Result<Vec<u8>, AgentTaskError> {
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
            handler: ExceptionHandler(IsTypeOfExceptionHandler::new(
                env,
                "ai/swim/server/agent/AgentException",
            )),
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
        msg: ByteBufferGuard,
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

        let (tx, rx) = mpsc::channel(8);

        let java_agent_ctx = Box::leak(Box::new(JavaAgentContext::new(tx)));

        let java_agent = agent_factory.agent_for(&ffi_context.env, route.as_str(), java_agent_ctx);
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

        task.boxed()
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
        let text_uri: Text = uri.into();

        if lane_kind_repr.map_like() {
            unimplemented!("map-like lanes")
        } else {
            let mut lane_conf = default_lane_config;
            lane_conf.transient = is_transient;
            let (tx, rx) = context.add_lane(text_uri.as_str(), kind, lane_conf).await?;

            if is_transient {
                lane_readers.push(LaneReader::value(lane_idx, rx));
                lane_writers.insert(lane_idx, tx);
            } else {
                init_tasks.push(run_lane_initializer(
                    ffi_context.clone(),
                    JavaValueLikeLaneInitializer::new(java_agent.clone(), spec.lane_idx),
                    lane_kind_repr,
                    (tx, rx),
                    WithLengthBytesCodec::default(),
                    lane_idx,
                ));
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

                if kind.map_like() {
                    unimplemented!()
                } else {
                    lane_readers.push(LaneReader::value(id, rx));
                    lane_writers.insert(id, tx);
                }
            }
            Err(e) => return Err(AgentInitError::LaneInitializationFailure(e)),
        }
    }

    Ok(FfiAgentTask {
        ffi_context,
        route,
        route_params,
        config,
        lane_identifiers,
        java_agent,
        lane_readers,
        lane_writers,
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

async fn run_lane_initializer<I, D>(
    context: FfiContext,
    initializer: I,
    kind: LaneKindRepr,
    io: (ByteWriter, ByteReader),
    decoder: D,
    id: i32,
) -> Result<InitializedLane, FrameIoError>
where
    I: JavaItemInitializer,
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

struct FfiAgentTask {
    ffi_context: FfiContext,
    route: RouteUri,
    route_params: HashMap<String, String>,
    config: AgentConfig,
    lane_identifiers: HashMap<i32, Text>,
    java_agent: JavaAgentRef,
    lane_readers: SelectAll<LaneReader>,
    lane_writers: HashMap<i32, ByteWriter>,
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
        } = self;

        info!("Running agent");

        let env = ffi_context.env.clone();
        java_agent.did_start(&env)?;

        loop {
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
                    handle_request(&ffi_context, &java_agent, id, request, &mut lane_writers)
                        .await?;
                }
                Some(RuntimeEvent::RequestError { id, error }) => {
                    let lane = lane_identifiers.get(&id).cloned().expect("Missing lane");
                    return Err(AgentTaskError::BadFrame { lane, error });
                }
                None => break,
            }
        }

        java_agent.did_stop(&env)?;

        Ok(())
    }
}

async fn handle_request(
    context: &FfiContext,
    agent_ref: &JavaAgentRef,
    lane_id: i32,
    request: TaggedLaneRequest,
    lane_writers: &mut HashMap<i32, ByteWriter>,
) -> Result<ControlFlow<()>, AgentTaskError> {
    let env = &context.env;
    let TaggedLaneRequest { request, .. } = request;

    match request {
        LaneRequest::Command(msg) => {
            trace!("Received a command request");
            let response = agent_ref.dispatch(&env, lane_id, msg)?;

            trace!("Handling lane response");

            forward_lane_responses(
                context,
                agent_ref,
                BytesMut::from_iter(response),
                lane_writers,
            )
            .await
        }
        LaneRequest::Sync(remote_id) => {
            trace!("Received a sync request");

            let response = agent_ref.sync(&env, lane_id, remote_id)?;
            forward_lane_responses(
                context,
                agent_ref,
                BytesMut::from_iter(response),
                lane_writers,
            )
            .await
        }
        LaneRequest::InitComplete => Ok(ControlFlow::Continue(())),
    }
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
                println!("Lane: {}: {:?}", lane_id, data.as_ref());

                match lane_writers.get_mut(&lane_id) {
                    Some(writer) => {
                        if writer.write_all(data.as_ref()).await.is_err() {
                            return Ok(ControlFlow::Break(()));
                        }
                    }
                    None => ctx.env.with_env(|scope| {
                        println!("Lane writers: {:?}", lane_writers);

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

#[derive(Debug)]
enum RuntimeEvent {
    Request { id: i32, request: TaggedLaneRequest },
    RequestError { id: i32, error: FrameIoError },
}
