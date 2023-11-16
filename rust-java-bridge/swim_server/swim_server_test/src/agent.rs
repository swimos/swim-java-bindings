use std::collections::{HashMap, VecDeque};
use std::io::ErrorKind;
use std::str::FromStr;
use std::sync::Arc;

use bytes::{Buf, BytesMut};
use futures_util::future::try_join3;
use futures_util::{Future, StreamExt};
use jni::objects::{GlobalRef, JObject, JValue};
use jni::sys::{jboolean, jbyteArray, jobject};
use swim_api::agent::Agent;
use swim_api::error::AgentInitError;
use swim_api::protocol::agent::{
    LaneRequestDecoder, MapLaneResponseDecoder, ValueLaneResponseDecoder,
};
use swim_api::protocol::map::{MapMessageDecoder, RawMapOperationDecoder};
use swim_api::protocol::WithLengthBytesCodec;
use swim_utilities::routing::route_uri::RouteUri;
use tokio::runtime::Builder;
use tokio::sync::{mpsc, Notify};
use tokio_util::codec::Decoder;
use tracing::debug;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::env::JavaEnv;
use jvm_sys::method::{InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethodDef};
use server_fixture::{
    Channels, LaneRequestDiscriminant, LaneResponseDiscriminant, TestAgentContext,
};
use swim_server_core::agent::context::JavaAgentContext;
use swim_server_core::agent::foreign::{GuestAgentFactory, JavaAgentRef, JavaAgentVTable};
use swim_server_core::agent::spec::AgentSpec;
use swim_server_core::agent::{GuestRuntimeRequest, JavaGuestConfig};
use swim_server_core::{server_fn, JavaGuestAgent};

server_fn! {
    pub fn agent_NativeTest_runNativeAgent(
        env,
        _class,
        inputs: jbyteArray,
        outputs:jbyteArray,
        agent_factory: jobject,
        agent_config: jbyteArray,
        ordered_responses: jboolean
    ) {
        // let filter = tracing_subscriber::EnvFilter::default().add_directive(LevelFilter::TRACE.into());
        // tracing_subscriber::fmt().with_env_filter(filter).init();

        let env = JavaEnv::new(env);
        let spec = match AgentSpec::try_from_jbyte_array::<()>(&env, agent_config) {
            Ok(spec) => spec,
            Err(_) => return,
        };

        let (inputs, outputs) = env.with_env(|scope| {
           (scope.convert_byte_array(inputs), scope.convert_byte_array(outputs))
        });

        let factory_obj = env.with_env(|scope| {
            if agent_factory.is_null() {
                scope.fatal_error("Bug: agent factory object is null");
            } else {
                unsafe { JObject::from_raw(agent_factory)}
            }
        });

        let runtime = Builder::new_multi_thread().enable_all().build().expect("Failed to build Tokio runtime");

        if ordered_responses == 1 {
            runtime.block_on(run_agent(env, spec, factory_obj, inputs, outputs, expect_in_order_envelopes));
        } else {
            runtime.block_on(run_agent(env, spec, factory_obj, inputs, outputs, out_of_order_envelopes));
        }
    }
}

#[derive(Default)]
struct TaggedDecoder<D>(D);

impl<D> Decoder for TaggedDecoder<D>
where
    D: Decoder<Error = std::io::Error>,
{
    type Item = (String, D::Item);
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        if !src.has_remaining() {
            return Ok(None);
        } else {
            let uri_len = src.get_i32() as usize;
            let lane_uri = std::str::from_utf8(src.split_to(uri_len).as_ref())
                .expect("Invalid lane URI")
                .to_string();
            Ok(self.0.decode(src)?.map(|it| (lane_uri, it)))
        }
    }
}

#[derive(Default)]
struct ResponseDecoder;

impl Decoder for ResponseDecoder {
    type Item = LaneResponseDiscriminant;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        match src.get_i8() {
            0 => {
                let mut decoder = ValueLaneResponseDecoder::default();
                decoder
                    .decode(src)
                    .map_err(|_| ErrorKind::InvalidData.into())
                    .map(|e| e.map(LaneResponseDiscriminant::Value))
            }
            1 => {
                let mut decoder = MapLaneResponseDecoder::default();
                decoder
                    .decode(src)
                    .map_err(|_| ErrorKind::InvalidData.into())
                    .map(|e| e.map(LaneResponseDiscriminant::Map))
            }
            _n => panic!("Invalid boolean for kind: {_n}"),
        }
    }
}

#[derive(Default)]
struct RequestDecoder;

impl Decoder for RequestDecoder {
    type Item = LaneRequestDiscriminant;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        match src.get_i8() {
            0 => {
                let mut decoder = LaneRequestDecoder::new(WithLengthBytesCodec::default());
                decoder
                    .decode(src)
                    .map_err(|_| ErrorKind::InvalidData.into())
                    .map(|e| e.map(LaneRequestDiscriminant::Value))
            }
            1 => {
                let mut decoder = LaneRequestDecoder::new(MapMessageDecoder::new(
                    RawMapOperationDecoder::default(),
                ));
                decoder
                    .decode(src)
                    .map_err(|_| ErrorKind::InvalidData.into())
                    .map(|e| e.map(LaneRequestDiscriminant::Map))
            }
            _n => panic!("Invalid boolean for kind: {_n}"),
        }
    }
}

#[derive(Debug, Clone)]
pub struct JavaSingleAgentFactory {
    new_agent_method: InitialisedJavaObjectMethod,
    factory: GlobalRef,
    vtable: Arc<JavaAgentVTable>,
}

impl JavaSingleAgentFactory {
    const NEW_AGENT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentFactory",
        "newInstance",
        "(J)Lai/swim/server/agent/AgentView;",
    );

    pub fn new(env: &JavaEnv, factory: GlobalRef) -> JavaSingleAgentFactory {
        JavaSingleAgentFactory {
            new_agent_method: env.initialise(Self::NEW_AGENT),
            factory,
            vtable: Arc::new(JavaAgentVTable::initialise(env)),
        }
    }
}

impl GuestAgentFactory for JavaSingleAgentFactory {
    type GuestAgent = JavaAgentRef;
    type Context = JavaEnv;

    fn agent_for(
        &self,
        env: Self::Context,
        uri: impl AsRef<str>,
        tx: mpsc::Sender<GuestRuntimeRequest>,
    ) -> Result<Self::GuestAgent, AgentInitError> {
        let node_uri = uri.as_ref();

        let JavaSingleAgentFactory {
            new_agent_method,
            factory,
            vtable,
        } = self;

        let agent = env.with_env(|scope| {
            let java_agent_ctx = Box::leak(Box::new(JavaAgentContext::new(env.clone(), tx)));
            let obj_ref = unsafe {
                scope.invoke(
                    new_agent_method.l().global_ref(),
                    factory.as_obj(),
                    &[JValue::Object(JObject::from_raw(
                        java_agent_ctx as *mut _ as u64 as jobject,
                    ))],
                )
            };
            JavaAgentRef::new(env.clone(), obj_ref, vtable.clone())
        });

        debug!(node_uri, "Created new java agent");

        Ok(agent)
    }
}

/// Runs an agent (defined by 'spec'), feeding it inputs from 'inputs' and asserting that the events
/// that it produces the correct outputs decoded from 'outputs'.
///
/// # Arguments:
/// `env`: Java environment
/// `spec`: the agent's specification.
/// `agent_factory`: a reference to the `AgentFactory` in Java for creating the agent.
/// `inputs`: a vector of bytes that will be decoded by a `TaggedDecoder::<RequestDecoder>` instance.
/// This contains the commands that will be forwarded to the agent. These commands may be multiple
/// messages for the same lane URI.
/// `outputs`: a vector of bytes that will be decoded by a `TaggedDecoder::<ResponseDecoder>`
/// instance. These are the expected responses that the agent will produce.
/// `response_validator`: a validator for envelopes produced by an agent. This can be used to provide
/// a validator which may expect the envelopes to be either in order or out-of-order.
///
/// # Errors:
/// If there is an error, then it will panic to ensure that the error is propagated to the JVM.
///
/// # Note:
/// This function needs to be blocked on by the callee. If there is an error, then it will panic to
/// ensure that the error is propagated to the JVM.
async fn run_agent<V, VFut>(
    env: JavaEnv,
    spec: AgentSpec,
    agent_factory: JObject<'_>,
    inputs: Vec<u8>,
    outputs: Vec<u8>,
    response_validator: V,
) where
    V: FnOnce(HashMap<String, VecDeque<LaneResponseDiscriminant>>, Channels) -> VFut,
    VFut: Future<Output = ()>,
{
    let factory = env
        .with_env(|scope| JavaSingleAgentFactory::new(&env, scope.new_global_ref(agent_factory)));
    let agent = JavaGuestAgent::new(env.clone(), spec, factory, JavaGuestConfig::java_default());
    let agent_context = TestAgentContext::default();
    let start_barrier = Arc::new(Notify::new());

    let producer_barrier = start_barrier.clone();
    let producer_channels = agent_context.channels();
    let producer_task = async move {
        producer_barrier.notified().await;

        let mut data = BytesMut::from_iter(inputs);
        let mut decoder = TaggedDecoder::<RequestDecoder>::default();

        loop {
            match decoder.decode(&mut data) {
                Ok(Some((uri, request))) => producer_channels.send(uri, request).await,
                Ok(None) => {
                    break;
                }
                Err(e) => panic!("Decode error: {:?}", e),
            }
        }

        Ok(())
    };

    let consumer_barrier = start_barrier.clone();
    let consumer_channels = agent_context.channels();
    let consumer_task = async move {
        consumer_barrier.notified().await;

        let mut data = BytesMut::from_iter(outputs);
        let mut decoder = TaggedDecoder::<ResponseDecoder>::default();
        let mut responses: HashMap<String, VecDeque<LaneResponseDiscriminant>> =
            HashMap::<String, VecDeque<LaneResponseDiscriminant>>::default();

        loop {
            match decoder.decode(&mut data) {
                Ok(Some((uri, response))) => {
                    responses.entry(uri).or_default().push_back(response);
                }
                Ok(None) => {
                    break;
                }
                Err(e) => panic!("Decode error: {:?}", e),
            }
        }

        response_validator(responses, consumer_channels).await;

        Ok(())
    };

    let agent_task = async move {
        let task = agent
            .run(
                RouteUri::from_str("agent").unwrap(),
                Default::default(),
                Default::default(),
                Box::new(agent_context.clone()),
            )
            .await
            .expect("Failed to initialise agent");

        start_barrier.notify_waiters();

        task.await
    };

    let result = try_join3(producer_task, consumer_task, agent_task).await;

    if let Err(e) = result {
        let lock = std::io::stderr().lock();
        eprintln!("Task error: {}", e);
        drop(lock);

        env.with_env(|scope| scope.throw_new("java/lang/RuntimeException", e.to_string()))
    }
}

async fn expect_in_order_envelopes(
    mut responses: HashMap<String, VecDeque<LaneResponseDiscriminant>>,
    mut channels: Channels,
) {
    while let Some((uri, response)) = channels.next().await {
        match responses.get_mut(&uri) {
            Some(expected) => {
                let expected_response = expected.pop_front().expect("Missing response");
                assert_eq!(expected_response, response);

                if expected.is_empty() {
                    responses.remove(&uri);
                }

                if responses.is_empty() {
                    break;
                }
            }
            None => {
                panic!("Missing lane response for: {uri}")
            }
        }
    }

    // Drop all of the agent's input channels so that it stops.
    channels.drop_all().await;
}

async fn out_of_order_envelopes(
    mut responses: HashMap<String, VecDeque<LaneResponseDiscriminant>>,
    mut channels: Channels,
) {
    while let Some((uri, response)) = channels.next().await {
        match responses.get_mut(&uri) {
            Some(expected) => match expected.iter().position(|e| e.eq(&response)) {
                Some(idx) => {
                    expected.remove(idx);

                    if expected.is_empty() {
                        responses.remove(&uri);
                    }

                    if responses.is_empty() {
                        break;
                    }
                }
                None => {
                    panic!("Unexpected response for lane ({uri}): {response:?}")
                }
            },
            None => {
                panic!("Missing lane response for: {uri}")
            }
        }
    }

    // Drop all of the agent's input channels so that it stops.
    channels.drop_all().await;
}
