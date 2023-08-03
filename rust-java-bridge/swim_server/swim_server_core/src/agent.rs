use std::cell::RefCell;
use std::collections::HashMap;
use std::convert::Infallible;
use std::error::Error;
use std::future::Future;
use std::iter::StepBy;
use std::mem::size_of;
use std::ops::Deref;
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
use jni::objects::{GlobalRef, JByteBuffer, JObject, JValue};
use jni::JNIEnv;
use swim_api::agent::{Agent, AgentConfig, AgentContext, AgentInitResult, LaneConfig, UplinkKind};
use swim_api::error::{AgentInitError, AgentTaskError, FrameIoError};
use swim_api::meta::lane::LaneKind;
use swim_api::protocol::agent::{
    LaneRequest, LaneRequestDecoder, LaneResponse, LaneResponseDecoder, LaneResponseEncoder,
    StoreInitMessage, StoreInitMessageDecoder, StoreInitialized, StoreInitializedCodec,
    ValueLaneResponseEncoder,
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
use tokio::io::AsyncWriteExt;
use tokio::select;
use tokio::task::spawn_blocking;
use tokio_util::codec::{BytesCodec, Decoder, Encoder, FramedRead, FramedWrite};
use tracing::{debug, error, info, trace};
use tracing_subscriber::fmt::init;

use bytebridge::{ByteCodec, FromBytesError};
use jvm_sys::vm::method::{
    ByteArray, InitialisedJavaObjectMethod, JavaCallback, JavaMethodExt, JavaObjectMethod,
    JavaObjectMethodDef, MethodDefinition, MethodResolver,
};
use jvm_sys::vm::utils::VmExt;
use jvm_sys::vm::{
    fallible_jni_call, jni_call, set_panic_hook, with_local_frame_null, IsTypeOfExceptionHandler,
    JniErrorKind, SharedVm, SpannedError,
};
use jvm_sys::EnvExt;

use crate::spec::{AgentSpec, LaneKindRepr, LaneSpec};
use crate::FfiContext;

type LaneCodec<T> = FramedRead<ByteReader, LaneRequestDecoder<T>>;
type ValueReaderCodec = LaneCodec<WithLengthBytesCodec>;
type MapReaderCodec = LaneCodec<MapMessageDecoder<RawMapOperationDecoder>>;

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
        "(Ljava/lang/String;)Lai/swim/server/agent/Agent;",
    );

    pub fn new(env: &JNIEnv, factory: GlobalRef, resolver: MethodResolver) -> AgentFactory {
        AgentFactory {
            new_agent_method: resolver.resolve(env, Self::NEW_AGENT),
            factory,
            vtable: Arc::new(JavaAgentVTable::initialise(env, resolver)),
        }
    }

    pub fn agent_for(&self, env: &JNIEnv, uri: impl AsRef<str>) -> Result<JavaAgentRef, JavaError> {
        let AgentFactory {
            new_agent_method,
            factory,
            vtable,
        } = self;
        let java_uri = env.new_string(uri.as_ref())?;

        with_local_frame_null(env, None, || {
            let result = new_agent_method.object().global_ref().invoke(
                env,
                factory.as_obj(),
                &[JValue::Object(java_uri.deref().clone())],
            );
            match result {
                Ok(obj_ref) => Ok(JavaAgentRef::new(obj_ref, vtable.clone())),
                Err(e) => Err(e),
            }
        })
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

    fn did_start(&self, env: &JNIEnv) -> Result<(), JavaError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        with_local_frame_null(env, None, || vtable.did_start(env, agent_obj.as_obj()))
    }

    fn did_stop(&self, env: &JNIEnv) -> Result<(), JavaError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        with_local_frame_null(env, None, || vtable.did_stop(env, agent_obj.as_obj()))
    }

    fn dispatch(
        &self,
        env: &JNIEnv,
        lane_name: &GlobalRef,
        msg: JByteBuffer,
    ) -> Result<Vec<u8>, JavaError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        with_local_frame_null(env, None, || {
            vtable.dispatch(env, agent_obj.as_obj(), lane_name.as_obj(), msg)
        })
    }

    fn init(&self, env: &JNIEnv, lane_name: &GlobalRef, msg: JByteBuffer) -> Result<(), JavaError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        with_local_frame_null(env, None, || {
            vtable.init(env, agent_obj.as_obj(), lane_name.as_obj(), msg)
        })
    }

    fn flush_state(&self, env: &JNIEnv) -> Result<Vec<u8>, JavaError> {
        let JavaAgentRef { agent_obj, vtable } = self;
        with_local_frame_null(env, None, || vtable.flush_state(env, agent_obj.as_obj()))
    }
}

#[derive(Debug)]
pub struct JavaAgentVTable {
    did_start: InitialisedJavaObjectMethod,
    did_stop: InitialisedJavaObjectMethod,
    dispatch: InitialisedJavaObjectMethod,
    init: InitialisedJavaObjectMethod,
    flush_state: InitialisedJavaObjectMethod,
}

impl JavaAgentVTable {
    const DID_START: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/Agent", "didStart", "()V");
    const DID_STOP: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/Agent", "didStop", "()V");
    const DISPATCH: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentModel",
        "dispatch",
        "(Ljava/lang/String;Ljava/nio/ByteBuffer;)[B",
    );
    const INIT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentModel",
        "init",
        "(Ljava/lang/String;Ljava/nio/ByteBuffer;)V",
    );
    const FLUSH_STATE: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentModel", "flushState", "()[B");

    fn initialise(env: &JNIEnv, resolver: MethodResolver) -> JavaAgentVTable {
        JavaAgentVTable {
            did_start: resolver.resolve(env, Self::DID_START),
            did_stop: resolver.resolve(env, Self::DID_STOP),
            dispatch: resolver.resolve(env, Self::DISPATCH),
            init: resolver.resolve(env, Self::INIT),
            flush_state: resolver.resolve(env, Self::FLUSH_STATE),
        }
    }

    fn did_start(&self, env: &JNIEnv, agent_obj: JObject) -> Result<(), JavaError> {
        self.did_start.void().invoke(env, agent_obj, &[])
    }

    fn did_stop(&self, env: &JNIEnv, agent_obj: JObject) -> Result<(), JavaError> {
        self.did_stop.void().invoke(env, agent_obj, &[])
    }

    fn dispatch(
        &self,
        env: &JNIEnv,
        agent_obj: JObject,
        lane_ref: JObject,
        msg: JByteBuffer,
    ) -> Result<Vec<u8>, JavaError> {
        self.dispatch.object().array::<ByteArray>().invoke(
            env,
            agent_obj,
            &[lane_ref.into(), msg.into()],
        )
    }

    fn flush_state(&self, env: &JNIEnv, agent_obj: JObject) -> Result<Vec<u8>, JavaError> {
        self.flush_state
            .object()
            .array::<ByteArray>()
            .invoke(env, agent_obj, &[])
    }

    fn init(
        &self,
        env: &JNIEnv,
        agent_obj: JObject,
        lane_ref: JObject,
        msg: JByteBuffer,
    ) -> Result<(), JavaError> {
        self.init
            .void()
            .invoke(env, agent_obj, &[lane_ref.into(), msg.into()])
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
        let env = ffi_context.vm.expect_env();
        let java_agent = match jni_call(&env, || agent_factory.agent_for(&env, route.as_str())) {
            Ok(obj) => obj,
            Err(e) => panic_any(e),
        };

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
    let mut lane_name_lookup = HashMap::new();
    let mut init_tasks = FuturesUnordered::default();
    let mut lane_readers = SelectAll::new();
    let mut item_writers = HashMap::new();

    {
        let env = ffi_context.vm.expect_env();

        for (idx, uri) in lane_specs.keys().enumerate() {
            let uri_string = env.new_string(uri.as_str()).unwrap();
            let uri_global_ref = env.new_global_ref(uri_string).unwrap();
            lane_name_lookup.insert(
                idx as u64,
                LaneName {
                    java: uri_global_ref,
                    str: uri.into(),
                },
            );
        }
    }

    for (idx, (uri, spec)) in lane_specs.into_iter().enumerate() {
        let idx = idx as u64;
        let LaneSpec {
            is_transient,
            lane_kind_repr,
        } = spec;
        let kind: LaneKind = lane_kind_repr.into();

        if lane_kind_repr.map_like() {
            unimplemented!()
        } else {
            let mut lane_conf = default_lane_config;
            lane_conf.transient = is_transient;
            let (tx, rx) = context.add_lane(uri.as_str(), kind, lane_conf).await?;

            if is_transient {
                lane_readers.push(LaneReader::value(idx, rx));
                item_writers.insert(idx, tx);
            } else {
                println!("Lane conf: {:?}", lane_conf);
                init_tasks.push(run_lane_initializer(
                    ffi_context.clone(),
                    JavaValueLikeLaneInitializer::new(
                        java_agent.clone(),
                        (&lane_name_lookup[&idx]).java.clone(),
                    ),
                    lane_kind_repr,
                    (tx, rx),
                    WithLengthBytesCodec::default(),
                    idx,
                ));
            }
        }
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
                    item_writers.insert(id, tx);
                }
            }
            Err(e) => panic!("{:?}", e),
        }
    }

    Ok(FfiAgentTask {
        ffi_context,
        route,
        route_params,
        config,
        lane_name_lookup,
        java_agent,
        lane_readers,
        item_writers,
    })
}

struct InitializedLane {
    kind: LaneKindRepr,
    io: (ByteWriter, ByteReader),
    id: u64,
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
    lane_name: GlobalRef,
}

impl JavaValueLikeLaneInitializer {
    pub fn new(agent_obj: JavaAgentRef, lane_name: GlobalRef) -> JavaValueLikeLaneInitializer {
        JavaValueLikeLaneInitializer {
            agent_obj,
            lane_name,
        }
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
        let JavaValueLikeLaneInitializer {
            agent_obj,
            lane_name,
        } = self;
        let JavaAgentRef { agent_obj, vtable } = agent_obj;
        let agent_obj = agent_obj.clone();
        let agent_vtable = vtable.clone();
        let lane_name = lane_name.clone();

        Box::pin(async move {
            println!("Value lane init wait");
            match try_last(stream).await? {
                Some(mut body) => {
                    println!("Init with: {:?}", body.as_ref());

                    let FfiContext { vm, .. } = context;
                    let env = vm.expect_env();
                    let agent_ptr = agent_obj.as_obj();
                    let buf = unsafe { env.new_direct_byte_buffer_exact(&mut body).unwrap() };

                    agent_vtable
                        .init(&env, agent_ptr, lane_name.as_obj(), buf)
                        .unwrap();
                    Ok(())
                }
                None => {
                    println!("Nothing to init with");
                    Ok(())
                }
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
    id: u64,
) -> Result<InitializedLane, FrameIoError>
where
    I: JavaItemInitializer,
    D: Decoder<Item = BytesMut> + Send + 'static,
    FrameIoError: From<D::Error>,
{
    println!("run_lane_initializer");
    let (mut tx, mut rx) = io;
    let stream = init_stream(&mut rx, decoder);
    match initializer.initialize(context, stream).await {
        Ok(()) => {
            println!("Init ok");
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
        Err(e) => {
            println!("Init error");
            Err(e)
        }
    }
}

#[derive(Clone)]
struct LaneName {
    java: GlobalRef,
    str: Text,
}

struct FfiAgentTask {
    ffi_context: FfiContext,
    route: RouteUri,
    route_params: HashMap<String, String>,
    config: AgentConfig,
    lane_name_lookup: HashMap<u64, LaneName>,
    java_agent: JavaAgentRef,
    lane_readers: SelectAll<LaneReader>,
    item_writers: HashMap<u64, ByteWriter>,
}

impl FfiAgentTask {
    async fn run(self) -> Result<(), AgentTaskError> {
        let FfiAgentTask {
            ffi_context,
            route,
            route_params,
            config,
            lane_name_lookup,
            java_agent,
            mut lane_readers,
            mut item_writers,
        } = self;

        info!("Running agent");
        {
            info!("Invoking Java agent did start lifecycle event");
            let env = ffi_context.vm.expect_env();
            java_agent.did_start(&env).unwrap();
        }

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
                    let lane_name = lane_name_lookup.get(&id).cloned().expect("Missing lane");
                    let mut writer = item_writers.get_mut(&id).expect("Missing lane writer");
                    if let Err(e) = handle_request(
                        &ffi_context,
                        &java_agent,
                        lane_name.java,
                        request,
                        &mut writer,
                    )
                    .await
                    {
                        match e {
                            FfiAgentError::Java(e) => {
                                panic!("{:?}", e)
                            }
                            FfiAgentError::Decode(error) => {
                                return Err(AgentTaskError::BadFrame {
                                    lane: lane_name.str,
                                    error,
                                })
                            }
                        }
                    }
                }
                Some(RuntimeEvent::RequestError { id, error }) => {}
                None => break,
            }
        }

        {
            let env = ffi_context.vm.expect_env();
            java_agent.did_stop(&env).unwrap();
        }

        Ok(())
    }
}

type BoxError = Box<dyn Error + Send + 'static>;

async fn handle_request(
    context: &FfiContext,
    agent_ref: &JavaAgentRef,
    lane_name_ref: GlobalRef,
    request: TaggedLaneRequest,
    lane_writer: &mut &mut ByteWriter,
) -> Result<(), FfiAgentError> {
    let TaggedLaneRequest { lane_type, request } = request;
    match request {
        LaneRequest::Command(mut msg) => {
            let result = {
                let env = context.vm.expect_env();
                let buffer = unsafe { env.new_direct_byte_buffer_exact(&mut msg) }?;
                trace!("Invoking Java agent dispatch method");
                jni_call(&env, || agent_ref.dispatch(&env, &lane_name_ref, buffer))
            };
            match result {
                Ok(ret) => {
                    match lane_type {
                        LaneType::Value => {
                            trace!("Handling lane response");

                            handle_response(context, agent_ref, ret, lane_writer).await?;
                        }
                        LaneType::Map => {
                            unimplemented!()
                        }
                    }

                    Ok(())
                }
                Err(e) => panic_any(e),
            }
        }
        LaneRequest::Sync(_remote_id) => {
            unimplemented!()
        }
        LaneRequest::InitComplete => Ok(()),
    }
}

struct AgentResponseDecoder<D> {
    fin: Option<bool>,
    delegate: LaneResponseDecoder<D>,
}

impl<D> AgentResponseDecoder<D> {
    fn new(delegate: D) -> AgentResponseDecoder<D> {
        AgentResponseDecoder {
            fin: None,
            delegate: LaneResponseDecoder::new(delegate),
        }
    }
}

impl<D> Decoder for AgentResponseDecoder<D>
where
    D: Decoder<Item = BytesMut>,
    D::Error: Into<FrameIoError>,
{
    type Item = ExecutionStep;
    type Error = FrameIoError;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        // let AgentResponseDecoder { fin, delegate } = self;
        // let remaining = src.remaining();
        //
        //
        //
        // if remaining < size_of::<i64>() {
        //     Ok(Some(ExecutionStep::Complete))
        // } else {
        //     let len = src.get_i64() as usize;
        //
        //     if src.remaining() < len {
        //         Ok(None)
        //     } else {
        //         Ok(Some(src.split_to(len)))
        //     }
        // }
        unimplemented!()
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

enum ExecutionStep {
    Yielded(BytesMut),
    Complete,
    MoreDataAvailable,
}

async fn handle_response(
    ctx: &FfiContext,
    agent_ref: &JavaAgentRef,
    mut returned_value: Vec<u8>,
    lane_writer: &mut &mut ByteWriter,
) -> Result<(), FfiAgentError> {
    loop {
        if returned_value.len() < 1 {
            return Ok(());
        }

        let (int_parts, rest) = returned_value.split_at(size_of::<i32>());
        println!("Int parts: {:?}", int_parts);
        println!("Rest: {:?}", rest);

        let len = i32::from_be_bytes(int_parts.try_into().unwrap()) as usize;
        println!("Len: {}", len);

        lane_writer.write_all(&rest[0..len]).await?;

        let (more_data, _) = returned_value.split_at(size_of::<i8>());

        if more_data[0] == 1u8 {
            debug!("Agent has specified that there is more data to fetch");
            let env = ctx.vm.expect_env();
            returned_value = agent_ref.flush_state(&env)?;
        } else {
            debug!("Agent response decode complete");
            return Ok(());
        }
    }
}

#[test]
fn t() {
    let mut encoder = ValueLaneResponseEncoder::new(WithLenReconEncoder);
    let mut buf = BytesMut::new();
    encoder
        .encode(LaneResponse::StandardEvent(13), &mut buf)
        .unwrap();
    println!("{:?}", buf.as_ref());
    println!("{}", buf.remaining());
}

enum LaneReaderCodec {
    Value(ValueReaderCodec),
    Map(MapReaderCodec),
}

impl LaneReaderCodec {
    fn value(reader: ByteReader) -> LaneReaderCodec {
        LaneReaderCodec::Value(LaneCodec::new(reader, LaneRequestDecoder::default()))
    }

    fn map(reader: ByteReader) -> LaneReaderCodec {
        LaneReaderCodec::Map(LaneCodec::new(reader, LaneRequestDecoder::default()))
    }
}

struct MapOperationBytesEncoder;

impl MapOperationBytesEncoder {
    const TAG_SIZE: usize = std::mem::size_of::<u8>();
    const LEN_SIZE: usize = std::mem::size_of::<u64>();

    const UPDATE: u8 = 0;
    const REMOVE: u8 = 1;

    const CLEAR: u8 = 2;
    const OVERSIZE_KEY: &'static str = "Key too large.";
    const OVERSIZE_RECORD: &'static str = "Record too large.";
}

impl Encoder<MapOperation<BytesMut, BytesMut>> for MapOperationBytesEncoder {
    type Error = std::io::Error;

    fn encode(
        &mut self,
        item: MapOperation<BytesMut, BytesMut>,
        dst: &mut BytesMut,
    ) -> Result<(), Self::Error> {
        match item {
            MapOperation::Update { key, value } => {
                let total_len = key.len() + value.len() + Self::LEN_SIZE + Self::TAG_SIZE;
                dst.reserve(total_len + Self::LEN_SIZE);
                dst.put_u64(u64::try_from(total_len).expect(Self::OVERSIZE_RECORD));
                dst.put_u8(Self::UPDATE);
                let key_len = u64::try_from(key.len()).expect(Self::OVERSIZE_KEY);
                dst.put_u64(key_len);
                dst.put(key);
                dst.put(value);
            }
            MapOperation::Remove { key } => {
                let total_len = key.len() + Self::TAG_SIZE;
                dst.reserve(total_len + Self::LEN_SIZE);
                dst.put_u64(u64::try_from(total_len).expect(Self::OVERSIZE_RECORD));
                dst.put_u8(Self::REMOVE);
                dst.put(key);
            }
            MapOperation::Clear => {
                dst.reserve(Self::LEN_SIZE + Self::TAG_SIZE);
                dst.put_u64(Self::TAG_SIZE as u64);
                dst.put_u8(Self::CLEAR);
            }
        }
        Ok(())
    }
}

impl Stream for LaneReaderCodec {
    type Item = (LaneType, Result<LaneRequest<BytesMut>, FrameIoError>);

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        match self.get_mut() {
            LaneReaderCodec::Value(ref mut inner) => Pin::new(inner)
                .poll_next(cx)
                .map(|r| r.map(|r| (LaneType::Value, r))),
            LaneReaderCodec::Map(ref mut inner) => match Pin::new(inner).poll_next(cx) {
                Poll::Ready(Some(Ok(op))) => {
                    let item = match op {
                        LaneRequest::Command(command) => {
                            let mut buf = BytesMut::new();
                            MapMessageEncoder::new(MapOperationBytesEncoder)
                                .encode(command, &mut buf)
                                .expect("Map encoding should be infallible");
                            LaneRequest::Command(buf)
                        }
                        LaneRequest::InitComplete => LaneRequest::InitComplete,
                        LaneRequest::Sync(id) => LaneRequest::Sync(id),
                    };
                    Poll::Ready(Some((LaneType::Map, Ok(item))))
                }
                Poll::Ready(Some(Err(e))) => Poll::Ready(Some((LaneType::Map, Err(e)))),
                Poll::Ready(None) => Poll::Ready(None),
                Poll::Pending => Poll::Pending,
            },
        }
    }
}

struct LaneWriter {
    idx: u64,
    writer: ByteWriter,
}

struct LaneReader {
    idx: u64,
    codec: LaneReaderCodec,
}

impl LaneReader {
    fn value(idx: u64, reader: ByteReader) -> LaneReader {
        LaneReader {
            idx,
            codec: LaneReaderCodec::value(reader),
        }
    }

    fn map(idx: u64, reader: ByteReader) -> LaneReader {
        LaneReader {
            idx,
            codec: LaneReaderCodec::map(reader),
        }
    }
}

#[derive(Debug)]
enum LaneType {
    Value,
    Map,
}

#[derive(Debug)]
struct TaggedLaneRequest {
    lane_type: LaneType,
    request: LaneRequest<BytesMut>,
}

impl Stream for LaneReader {
    type Item = (u64, Result<TaggedLaneRequest, FrameIoError>);

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
    // WriteComplete {
    //     writer: ItemWriter,
    //     result: Result<(), std::io::Error>,
    // },
    Request { id: u64, request: TaggedLaneRequest },
    RequestError { id: u64, error: FrameIoError },
}
