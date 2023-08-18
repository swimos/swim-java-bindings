use crate::TestAgentContext;
use bytes::{Buf, BytesMut};
use futures_util::future::try_join3;
use futures_util::StreamExt;
use jni::objects::{JObject, JString};
use jni::sys::jbyteArray;
use jvm_sys::bridge::JniByteCodec;
use jvm_sys::env::JavaEnv;
use std::collections::HashMap;
use std::io::ErrorKind;
use std::str::FromStr;
use std::sync::Arc;
use swim_api::agent::Agent;
use swim_api::protocol::agent::{
    LaneRequest, LaneRequestDecoder, LaneResponse, LaneResponseDecoder, ValueLaneResponseDecoder,
};
use swim_api::protocol::WithLengthBytesCodec;
use swim_server_core::spec::{AgentSpec, PlaneSpec};
use swim_server_core::{server_fn, AgentFactory, FfiAgentDef, FfiContext};
use swim_utilities::routing::route_uri::RouteUri;
use tokio::runtime::Builder;
use tokio::sync::Notify;
use tokio_util::codec::Decoder;

server_fn! {
    agent_AgentTest_runNativeAgent(
        env,
        _class,
        inputs: jbyteArray,
        outputs:jbyteArray,
        plane_obj: JObject,
        config: jbyteArray,
    ) {
        let env = JavaEnv::new(env);
        let spec = match PlaneSpec::try_from_jbyte_array::<()>(&env, config) {
            Ok(mut spec) => spec.agent_specs.remove("agent").expect("Missing agent definition"),
            Err(_) => return,
        };

        let (inputs, outputs) = env.with_env(|scope| {
           (scope.convert_byte_array(inputs), scope.convert_byte_array(outputs))
        });

        let runtime = Builder::new_multi_thread().enable_all().build().expect("Failed to build Tokio runtime");
        runtime.block_on(run_agent(env, spec, plane_obj, inputs, outputs));
    }
}

struct I32Decoder;

impl Decoder for I32Decoder {
    type Item = i32;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        if src.remaining() >= std::mem::size_of::<i32>() {
            Ok(Some(src.get_i32()))
        } else {
            Ok(None)
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
    type Item = LaneResponse<BytesMut>;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        let mut decoder = ValueLaneResponseDecoder::default();
        decoder
            .decode(src)
            .map_err(|_| ErrorKind::InvalidData.into())
    }
}

#[derive(Default)]
struct RequestDecoder;

impl Decoder for RequestDecoder {
    type Item = LaneRequest<BytesMut>;
    type Error = std::io::Error;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        let mut decoder = LaneRequestDecoder::new(WithLengthBytesCodec::default());
        decoder
            .decode(src)
            .map_err(|_| ErrorKind::InvalidData.into())
    }
}

async fn run_agent(
    env: JavaEnv,
    spec: AgentSpec,
    plane_obj: JObject<'_>,
    inputs: Vec<u8>,
    outputs: Vec<u8>,
) {
    let ffi_context = FfiContext::new(env.clone());
    let factory = env.with_env(|scope| AgentFactory::new(&env, scope.new_global_ref(plane_obj)));
    let agent = FfiAgentDef::new(ffi_context, spec, factory);
    let agent_context = TestAgentContext::default();
    let start_barrier = Arc::new(Notify::new());

    let producer_barrier = start_barrier.clone();
    let producer_channels = agent_context.channels.clone();
    let producer_task = async move {
        producer_barrier.notified().await;

        let mut data = BytesMut::from_iter(inputs);
        let mut decoder = TaggedDecoder::<RequestDecoder>::default();
        loop {
            match decoder.decode(&mut data) {
                Ok(Some((uri, request))) => producer_channels.send(uri, request).await,
                Ok(None) => break,
                Err(e) => panic!("Decode error: {:?}", e),
            }
        }

        Ok(())
    };

    let consumer_barrier = start_barrier.clone();
    let mut consumer_channels = agent_context.channels.clone();
    let consumer_task = async move {
        consumer_barrier.notified().await;

        let mut data = BytesMut::from_iter(outputs);
        let mut decoder = TaggedDecoder::<ResponseDecoder>::default();
        let mut responses = HashMap::<String, LaneResponse<BytesMut>>::default();

        loop {
            match decoder.decode(&mut data) {
                Ok(Some((uri, request))) => {
                    responses.insert(uri, request);
                }
                Ok(None) => break,
                Err(e) => panic!("Decode error: {:?}", e),
            }
        }

        while let Some((uri, response)) = consumer_channels.next().await {
            match responses.remove(&uri) {
                Some(expected) => {
                    assert_eq!(expected, response)
                }
                None => {
                    panic!("Missing lane response for: {uri}")
                }
            }
        }

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
        env.with_env(|scope| scope.throw_new("java/lang/RuntimeException", e.to_string()))
    }
}
