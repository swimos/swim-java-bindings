use std::collections::HashMap;
use std::num::NonZeroUsize;
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll, Waker};

use bytes::BytesMut;
use futures::{ready, Stream};
use futures_util::future::BoxFuture;
use futures_util::stream::FuturesUnordered;
use futures_util::{FutureExt, SinkExt, StreamExt};
use jni::sys::jbyteArray;
use swim_api::agent::{AgentContext, LaneConfig, UplinkKind};
use swim_api::downlink::DownlinkKind;
use swim_api::error::{AgentRuntimeError, DownlinkRuntimeError, OpenStoreError};
use swim_api::meta::lane::LaneKind;
use swim_api::protocol::agent::{
    LaneRequest, LaneRequestEncoder, LaneResponse, MapLaneResponseDecoder, ValueLaneResponseDecoder,
};
use swim_api::protocol::map::{MapMessage, RawMapOperation, RawMapOperationMut};
use swim_api::store::StoreKind;
use swim_utilities::io::byte_channel::{byte_channel, ByteReader, ByteWriter};
use swim_utilities::non_zero_usize;
use tokio::sync::{Mutex, MutexGuard, TryLockError};
use tokio_util::codec::{Encoder, FramedRead, FramedWrite};
use tokio_util::either::Either;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::env::JavaEnv;
use jvm_sys::null_pointer_check_abort;
use swim_server_core::server_fn;
use swim_server_core::spec::PlaneSpec;

mod agent;

const BUFFER_SIZE: NonZeroUsize = non_zero_usize!(128);

server_fn! {
    fn SwimServerTest_forPlane(
        env,
        _class,
        config: jbyteArray
    ) {
        null_pointer_check_abort!(env, config);

        let env = JavaEnv::new(env);

        let _r = PlaneSpec::try_from_jbyte_array::<()>(&env, config);
    }
}

#[derive(Default, Debug, Clone)]
struct TestAgentContext {
    channels: Channels,
}

impl AgentContext for TestAgentContext {
    fn add_lane(
        &self,
        name: &str,
        lane_kind: LaneKind,
        _config: LaneConfig,
    ) -> BoxFuture<'static, Result<(ByteWriter, ByteReader), AgentRuntimeError>> {
        let channels = self.channels.inner.clone();
        let lane_uri = name.to_string();
        async move {
            let guard = &mut *channels.lock().await;
            let (tx_in, rx_in) = byte_channel(BUFFER_SIZE);
            let (tx_out, rx_out) = byte_channel(BUFFER_SIZE);

            match lane_kind.uplink_kind() {
                UplinkKind::Value => guard.push_value_channel(tx_in, rx_out, lane_uri.to_string()),
                UplinkKind::Map => guard.push_map_channel(tx_in, rx_out, lane_uri.to_string()),
                UplinkKind::Supply => {
                    panic!("Unexpected supply uplink")
                }
            }

            Ok((tx_out, rx_in))
        }
        .boxed()
    }

    fn open_downlink(
        &self,
        _host: Option<&str>,
        _node: &str,
        _lane: &str,
        _kind: DownlinkKind,
    ) -> BoxFuture<'static, Result<(ByteWriter, ByteReader), DownlinkRuntimeError>> {
        panic!("Unexpected open downlink invocation")
    }

    fn add_store(
        &self,
        _name: &str,
        _kind: StoreKind,
    ) -> BoxFuture<'static, Result<(ByteWriter, ByteReader), OpenStoreError>> {
        panic!("Unexpected add store invocation")
    }
}

#[derive(Default, Debug, Clone)]
struct Channels {
    inner: Arc<Mutex<ChannelsInner>>,
}

impl Channels {
    fn poll_guard(&self, cx: &mut Context<'_>) -> Poll<MutexGuard<ChannelsInner>> {
        let mut fut = self.inner.lock();
        let mut pin = unsafe { Pin::new_unchecked(&mut fut) };
        match pin.poll_unpin(cx) {
            Poll::Ready(guard) => Poll::Ready(guard),
            Poll::Pending => Poll::Pending,
        }
    }

    async fn send<R: Into<LaneRequestDiscriminant>>(&self, lane_uri: String, request: R) {
        let mut guard = self.inner.lock().await;
        let inner = &mut *guard;
        let writer = inner.writers.get_mut(&lane_uri).expect("Missing channel");
        let mut framed = FramedWrite::new(writer, LaneRequestDiscriminantEncoder);

        framed.send(request.into()).await.expect("Channel closed");
    }

    async fn drop_all(&self) {
        let mut guard = self.inner.lock().await;
        let inner = &mut *guard;
        inner.writers.clear();
        inner.read_demux.clear();
    }
}

#[derive(Debug, PartialEq)]
pub enum LaneResponseDiscriminant {
    Value(LaneResponse<BytesMut>),
    Map(LaneResponse<RawMapOperationMut>),
}

impl LaneResponseDiscriminant {
    fn expect_value(self) -> LaneResponse<BytesMut> {
        match self {
            LaneResponseDiscriminant::Value(r) => r,
            LaneResponseDiscriminant::Map(_) => {
                panic!("Expected a value message")
            }
        }
    }

    fn expect_map(self) -> LaneResponse<RawMapOperationMut> {
        match self {
            LaneResponseDiscriminant::Value(_) => {
                panic!("Expected a map message")
            }
            LaneResponseDiscriminant::Map(r) => r,
        }
    }
}

pub struct LaneRequestDiscriminantEncoder;

impl Encoder<LaneRequestDiscriminant> for LaneRequestDiscriminantEncoder {
    type Error = std::io::Error;

    fn encode(
        &mut self,
        item: LaneRequestDiscriminant,
        dst: &mut BytesMut,
    ) -> Result<(), Self::Error> {
        match item {
            LaneRequestDiscriminant::Value(r) => LaneRequestEncoder::value().encode(r, dst),
            LaneRequestDiscriminant::Map(r) => LaneRequestEncoder::map().encode(r, dst),
        }
    }
}

#[derive(Debug)]
pub enum LaneRequestDiscriminant {
    Value(LaneRequest<BytesMut>),
    Map(LaneRequest<MapMessage<BytesMut, BytesMut>>),
}

impl From<LaneRequest<BytesMut>> for LaneRequestDiscriminant {
    fn from(value: LaneRequest<BytesMut>) -> Self {
        LaneRequestDiscriminant::Value(value)
    }
}

impl From<LaneRequest<MapMessage<BytesMut, BytesMut>>> for LaneRequestDiscriminant {
    fn from(value: LaneRequest<MapMessage<BytesMut, BytesMut>>) -> Self {
        LaneRequestDiscriminant::Map(value)
    }
}

impl LaneRequestDiscriminant {
    fn expect_value(self) -> LaneRequest<BytesMut> {
        match self {
            LaneRequestDiscriminant::Value(r) => r,
            LaneRequestDiscriminant::Map(_) => {
                panic!("Expected a value message")
            }
        }
    }

    fn expect_map(self) -> LaneRequest<MapMessage<BytesMut, BytesMut>> {
        match self {
            LaneRequestDiscriminant::Value(_) => {
                panic!("Expected a map message")
            }
            LaneRequestDiscriminant::Map(r) => r,
        }
    }
}

impl Stream for Channels {
    type Item = (String, LaneResponseDiscriminant);

    fn poll_next(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        loop {
            let mut guard = ready!(self.poll_guard(cx));
            let inner = &mut *guard;

            match ready!(Pin::new(&mut inner.read_demux).poll_next(cx)) {
                Some(Some((uri, reader, resp))) => {
                    return match resp {
                        LaneResponseDiscriminant::Value(r) => {
                            inner.push_value_reader(reader, uri.clone());
                            Poll::Ready(Some((uri, LaneResponseDiscriminant::Value(r))))
                        }
                        LaneResponseDiscriminant::Map(r) => {
                            inner.push_map_reader(reader, uri.clone());
                            Poll::Ready(Some((uri, LaneResponseDiscriminant::Map(r))))
                        }
                    }
                }
                Some(None) => {
                    if inner.read_demux.is_empty() {
                        return Poll::Pending;
                    }
                }
                None => return Poll::Ready(None),
            }
        }
    }
}

#[derive(Debug, Default)]
struct ChannelsInner {
    read_demux: FuturesUnordered<
        BoxFuture<'static, Option<(String, ByteReader, LaneResponseDiscriminant)>>,
    >,
    writers: HashMap<String, ByteWriter>,
}

impl ChannelsInner {
    fn push_value_channel(&mut self, tx: ByteWriter, rx: ByteReader, lane_uri: String) {
        self.push_value_reader(rx, lane_uri.clone());
        self.writers.insert(lane_uri, tx);
    }

    fn push_map_channel(&mut self, tx: ByteWriter, rx: ByteReader, lane_uri: String) {
        self.push_map_reader(rx, lane_uri.clone());
        self.writers.insert(lane_uri, tx);
    }

    fn push_value_reader(&mut self, rx: ByteReader, lane_uri: String) {
        self.read_demux.push(Box::pin(async move {
            let mut decoder = FramedRead::new(rx, ValueLaneResponseDecoder::default());
            match decoder.next().await {
                Some(Ok(response)) => Some((
                    lane_uri,
                    decoder.into_inner(),
                    LaneResponseDiscriminant::Value(response),
                )),
                None | Some(Err(_)) => None,
            }
        }));
    }

    fn push_map_reader(&mut self, rx: ByteReader, lane_uri: String) {
        self.read_demux.push(Box::pin(async move {
            let mut decoder = FramedRead::new(rx, MapLaneResponseDecoder::default());
            match decoder.next().await {
                Some(Ok(response)) => Some((
                    lane_uri,
                    decoder.into_inner(),
                    LaneResponseDiscriminant::Map(response),
                )),
                None | Some(Err(_)) => None,
            }
        }));
    }
}

#[cfg(test)]
mod tests {
    use bytes::{Buf, BytesMut};
    use futures_util::{SinkExt, StreamExt};
    use swim_api::agent::AgentContext;
    use swim_api::meta::lane::LaneKind;
    use swim_api::protocol::agent::{
        LaneResponse, LaneResponseDecoder, LaneResponseEncoder, ValueLaneResponseEncoder,
    };
    use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
    use swim_utilities::trigger::trigger;
    use tokio::join;
    use tokio_util::codec::{Decoder, Encoder, FramedRead, FramedWrite};

    use crate::TestAgentContext;

    const LANE_REG_FAILURE: &str = "Failed to register lane";
    const CLOSED_CHANNEL: &str = "Channel closed early";
    const INVALID_DATA: &str = "Channel received invalid data";
    const MISSING_CHANNEL: &str = "Missing channel";

    struct BytesCodec;

    impl Encoder<BytesMut> for BytesCodec {
        type Error = std::io::Error;

        fn encode(&mut self, item: BytesMut, dst: &mut BytesMut) -> Result<(), Self::Error> {
            dst.extend_from_slice(item.as_ref());
            Ok(())
        }
    }

    impl Decoder for BytesCodec {
        type Item = BytesMut;
        type Error = std::io::Error;

        fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
            if src.has_remaining() {
                let rem = src.remaining();
                Ok(Some(src.split_to(rem)))
            } else {
                Ok(None)
            }
        }
    }

    async fn round_trip(elem: i32, tx: ByteWriter, rx: ByteReader) {
        let mut encoder = FramedWrite::new(tx, ValueLaneResponseEncoder::default());
        encoder
            .send(LaneResponse::StandardEvent(elem))
            .await
            .expect(CLOSED_CHANNEL);

        let mut decoder = FramedRead::new(rx, LaneResponseDecoder::new(BytesCodec));
        let received = decoder
            .next()
            .await
            .expect(CLOSED_CHANNEL)
            .expect(INVALID_DATA);

        assert_eq!(
            received,
            LaneResponse::StandardEvent(BytesMut::from_iter(format!("{}", elem).as_bytes()))
        );
    }

    #[tokio::test]
    async fn read_demux() {
        let ctx = TestAgentContext::default();
        let agent_ctx = ctx.clone();
        let (stop_tx, stop_rx) = trigger();

        let agent = async move {
            let (tx, rx) = agent_ctx
                .add_lane("mock", LaneKind::Value, Default::default())
                .await
                .expect(LANE_REG_FAILURE);

            round_trip(13, tx, rx).await;

            let (tx, rx) = agent_ctx
                .add_lane("mock2", LaneKind::Value, Default::default())
                .await
                .expect(LANE_REG_FAILURE);

            round_trip(14, tx, rx).await;

            stop_tx.trigger();
        };

        let peer = async move {
            let inner = ctx.channels.inner.clone();
            let mut stream = ctx.channels.take_until(stop_rx);

            while let Some((uri, response)) = stream.next().await {
                let mut guard = inner.lock().await;
                let inner = &mut *guard;

                let tx = inner.writers.get_mut(&uri).expect(MISSING_CHANNEL);

                let mut encoder = FramedWrite::new(tx, LaneResponseEncoder::new(BytesCodec));
                encoder
                    .send(response.expect_value())
                    .await
                    .expect(CLOSED_CHANNEL);
            }
        };

        join!(agent, peer);
    }
}
