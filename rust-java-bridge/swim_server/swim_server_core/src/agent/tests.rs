use std::collections::HashMap;
use std::fmt::Debug;
use std::future::Future;
use std::mem::forget;
use std::str::FromStr;
use std::sync::Arc;
use std::time::Duration;

use bytes::{BufMut, BytesMut};
use futures_util::future::{try_join, BoxFuture};
use futures_util::StreamExt;
use parking_lot::Mutex;
use swim_api::agent::Agent;
use swim_api::error::{AgentInitError, AgentTaskError};
use swim_api::protocol::agent::{
    LaneRequest, LaneResponse, LaneResponseEncoder, StoreInitMessage, StoreInitMessageEncoder,
};
use swim_api::protocol::WithLengthBytesCodec;
use swim_utilities::routing::route_uri::RouteUri;
use tokio::sync::{mpsc, oneshot};
use tokio_util::codec::Encoder;
use uuid::Uuid;

use interval_stream::ScheduleDef;
use server_fixture::{Channels, TestAgentContext};

use crate::agent::foreign::{AgentFactory, AgentVTable, RuntimeContext};
use crate::agent::spec::{AgentSpec, LaneKindRepr, LaneSpec};
use crate::agent::AgentRuntimeRequest;
use crate::FfiAgentDef;

#[derive(thiserror::Error, Debug)]
#[error("Fatal error: {0}")]
struct FatalError(String);

#[derive(Clone)]
struct TestRuntimeContext;

impl RuntimeContext for TestRuntimeContext {
    fn fatal_error(&self, cause: impl ToString) -> AgentTaskError {
        AgentTaskError::UserCodeError(Box::new(FatalError(cause.to_string())))
    }
}

struct TestAgentFactoryInner {
    uri: String,
    sender: VTableChannelSender,
    runtime_provider: oneshot::Sender<mpsc::Sender<AgentRuntimeRequest>>,
}

struct TestAgentFactory {
    inner: Arc<Mutex<Option<TestAgentFactoryInner>>>,
}

impl TestAgentFactory {
    fn new(
        uri: String,
        sender: VTableChannelSender,
        runtime_provider: oneshot::Sender<mpsc::Sender<AgentRuntimeRequest>>,
    ) -> TestAgentFactory {
        TestAgentFactory {
            inner: Arc::new(Mutex::new(Some(TestAgentFactoryInner {
                uri,
                sender,
                runtime_provider,
            }))),
        }
    }
}

impl AgentFactory for TestAgentFactory {
    type VTable = TestAgentVTable;
    type Context = TestRuntimeContext;

    fn agent_for(
        &self,
        _ctx: Self::Context,
        uri: impl AsRef<str>,
        runtime_tx: mpsc::Sender<AgentRuntimeRequest>,
    ) -> Result<Self::VTable, AgentInitError> {
        let inner = &mut *self.inner.lock();
        let requested_uri = uri.as_ref();
        match inner.take() {
            Some(TestAgentFactoryInner {
                uri,
                sender,
                runtime_provider,
            }) => {
                if requested_uri == uri {
                    runtime_provider
                        .send(runtime_tx)
                        .expect("Failed to provide runtime channel");
                    Ok(TestAgentVTable { channel: sender })
                } else {
                    panic!("Unknown node URI requested: {}", requested_uri);
                }
            }
            None => {
                panic!("Agent requested more than once: {}", requested_uri)
            }
        }
    }
}

type PromiseSender<T> = oneshot::Sender<Result<T, AgentTaskError>>;

#[derive(Debug)]
enum VTableRequest {
    DidStart {
        promise: PromiseSender<()>,
    },
    DidStop {
        promise: PromiseSender<()>,
    },
    Dispatch {
        lane_id: i32,
        buffer: BytesMut,
        promise: PromiseSender<Vec<u8>>,
    },
    Sync {
        lane_id: i32,
        remote: Uuid,
        promise: PromiseSender<Vec<u8>>,
    },
    Init {
        lane_id: i32,
        msg: BytesMut,
        promise: PromiseSender<()>,
    },
    FlushState {
        promise: PromiseSender<Vec<u8>>,
    },
    RunTask {
        id_msb: i64,
        id_lsb: i64,
        promise: PromiseSender<()>,
    },
}

struct VTableChannelReceiver {
    rx: mpsc::Receiver<VTableRequest>,
}

impl VTableChannelReceiver {
    async fn did_start(&mut self, result: Result<(), AgentTaskError>) {
        match self.rx.recv().await {
            Some(VTableRequest::DidStart { promise }) => {
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected a did start request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }

    async fn did_stop(&mut self, result: Result<(), AgentTaskError>) {
        match self.rx.recv().await {
            Some(VTableRequest::DidStop { promise }) => {
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected a did stop request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }

    async fn dispatch(
        &mut self,
        expected_lane_id: i32,
        expected_buffer: BytesMut,
        result: Result<Vec<u8>, AgentTaskError>,
    ) {
        match self.rx.recv().await {
            Some(VTableRequest::Dispatch {
                lane_id,
                buffer,
                promise,
            }) => {
                assert_eq!(expected_lane_id, lane_id);
                assert_eq!(expected_buffer.as_ref(), buffer.as_ref());
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected a dispatch request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }

    async fn sync(
        &mut self,
        expected_lane_id: i32,
        expected_remote: Uuid,
        result: Result<Vec<u8>, AgentTaskError>,
    ) {
        match self.rx.recv().await {
            Some(VTableRequest::Sync {
                lane_id,
                remote,
                promise,
            }) => {
                assert_eq!(expected_lane_id, lane_id);
                assert_eq!(expected_remote, remote);
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected a sync request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }

    async fn init(
        &mut self,
        expected_lane_id: i32,
        expected_payload: BytesMut,
        result: Result<(), AgentTaskError>,
    ) {
        match self.rx.recv().await {
            Some(VTableRequest::Init {
                lane_id,
                msg,
                promise,
            }) => {
                assert_eq!(expected_lane_id, lane_id);
                assert_eq!(expected_payload, msg);
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected an init request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }

    async fn flush_state(&mut self, result: Result<Vec<u8>, AgentTaskError>) {
        match self.rx.recv().await {
            Some(VTableRequest::FlushState { promise }) => {
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected a flush state request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }

    async fn run_task(
        &mut self,
        expected_id_msb: i64,
        expected_id_lsb: i64,
        result: Result<(), AgentTaskError>,
    ) {
        match self.rx.recv().await {
            Some(VTableRequest::RunTask {
                id_msb,
                id_lsb,
                promise,
            }) => {
                assert_eq!(expected_id_msb, id_msb);
                assert_eq!(expected_id_lsb, id_lsb);
                promise.send(result).expect("VTable channel dropped")
            }
            Some(req) => {
                panic!("Expected a run task request. Received: {:?}", req)
            }
            None => {
                panic!("VTable channel dropped")
            }
        }
    }
}

#[derive(Clone)]
struct VTableChannelSender {
    tx: mpsc::Sender<VTableRequest>,
}

impl VTableChannelSender {
    async fn send<F, T>(&self, f: F) -> Result<T, AgentTaskError>
    where
        F: FnOnce(oneshot::Sender<Result<T, AgentTaskError>>) -> VTableRequest,
    {
        let (promise_tx, promise_rx) = oneshot::channel();
        self.tx
            .send(f(promise_tx))
            .await
            .expect("VTable channel dropped");
        promise_rx.await.expect("VTable request dropped")
    }
}

fn vtable_channel() -> (VTableChannelSender, VTableChannelReceiver) {
    let (tx, rx) = mpsc::channel(128);
    (VTableChannelSender { tx }, VTableChannelReceiver { rx })
}

struct TestAgentVTable {
    channel: VTableChannelSender,
}

impl AgentVTable for TestAgentVTable {
    type Suspended<O> = BoxFuture<'static,Result<O, AgentTaskError>>
    where
        O: Send;

    fn did_start(&self) -> Self::Suspended<()> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::DidStart { promise })
                .await
        })
    }

    fn did_stop(&self) -> Self::Suspended<()> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::DidStop { promise })
                .await
        })
    }

    fn dispatch(&mut self, lane_id: i32, buffer: BytesMut) -> Self::Suspended<Vec<u8>> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::Dispatch {
                    lane_id,
                    buffer,
                    promise,
                })
                .await
        })
    }

    fn sync(&self, lane_id: i32, remote: Uuid) -> Self::Suspended<Vec<u8>> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::Sync {
                    lane_id,
                    remote,
                    promise,
                })
                .await
        })
    }

    fn init(&mut self, lane_id: i32, msg: BytesMut) -> Self::Suspended<()> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::Init {
                    lane_id,
                    promise,
                    msg,
                })
                .await
        })
    }

    fn flush_state(&self) -> Self::Suspended<Vec<u8>> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::FlushState { promise })
                .await
        })
    }

    fn run_task(&self, id_msb: i64, id_lsb: i64) -> Self::Suspended<()> {
        let channel = self.channel.clone();
        Box::pin(async move {
            channel
                .send(|promise| VTableRequest::RunTask {
                    id_msb,
                    promise,
                    id_lsb,
                })
                .await
        })
    }
}

struct IdentifiedLaneResponse<T> {
    lane_id: i32,
    response: LaneResponse<T>,
}

impl<T> IdentifiedLaneResponse<T> {
    pub fn new(lane_id: i32, response: LaneResponse<T>) -> IdentifiedLaneResponse<T> {
        IdentifiedLaneResponse { lane_id, response }
    }
}

struct IdentifiedLaneResponseEncoder;

impl<T> Encoder<IdentifiedLaneResponse<T>> for IdentifiedLaneResponseEncoder
where
    T: AsRef<[u8]>,
{
    type Error = std::io::Error;

    fn encode(
        &mut self,
        item: IdentifiedLaneResponse<T>,
        dst: &mut BytesMut,
    ) -> Result<(), Self::Error> {
        let IdentifiedLaneResponse { lane_id, response } = item;
        dst.put_i32(lane_id);

        let mut temp = BytesMut::new();
        LaneResponseEncoder::new(WithLengthBytesCodec).encode(response, &mut temp)?;
        dst.put_u32(temp.len() as u32);
        dst.extend_from_slice(temp.as_ref());
        Ok(())
    }
}

#[repr(u8)]
#[derive(Copy, Clone)]
enum DispatchStatus {
    Complete = 0,
    Incomplete = 1,
}

struct DispatchEncoder<I> {
    status: DispatchStatus,
    delegate: I,
}

impl<I> DispatchEncoder<I> {
    pub fn new(status: DispatchStatus, delegate: I) -> DispatchEncoder<I> {
        DispatchEncoder { status, delegate }
    }
}

impl<I, E> Encoder<Vec<I>> for DispatchEncoder<E>
where
    E: Encoder<I, Error = std::io::Error>,
{
    type Error = std::io::Error;

    fn encode(&mut self, item: Vec<I>, dst: &mut BytesMut) -> Result<(), Self::Error> {
        dst.put_i8(self.status as i8);
        for it in item {
            self.delegate.encode(it, dst)?;
        }
        Ok(())
    }
}

fn encode_to_vec<E, T>(item: T, mut encoder: E) -> Vec<u8>
where
    E: Encoder<T>,
    E::Error: Debug,
{
    let mut dst = BytesMut::new();
    encoder
        .encode(item, &mut dst)
        .expect("Failed to encode item");
    dst.to_vec()
}

async fn expect_sync_event(channels: &mut Channels, lane_uri: &str, id: Uuid, value: BytesMut) {
    let (actual_lane_uri, actual_response) = channels.next().await.expect("Expected a sync event");
    assert_eq!(actual_lane_uri, lane_uri);
    assert_eq!(actual_response, LaneResponse::SyncEvent(id, value).into());
}

async fn expect_value_synced(channels: &mut Channels, lane_uri: &str, id: Uuid) {
    let (actual_lane_uri, actual_response) =
        channels.next().await.expect("Expected a synced event");
    assert_eq!(actual_lane_uri, lane_uri);
    assert_eq!(actual_response, LaneResponse::<BytesMut>::Synced(id).into());
}

async fn expect_event(channels: &mut Channels, lane_uri: &str, expected: BytesMut) {
    let (actual_lane_uri, actual_response) = channels.next().await.expect("Expected an event");
    assert_eq!(actual_lane_uri, lane_uri);
    assert_eq!(
        actual_response,
        LaneResponse::<BytesMut>::StandardEvent(expected).into()
    );
}

async fn run_agent<F, Fut>(
    spec: AgentSpec,
    sender: VTableChannelSender,
    test: F,
) -> Result<(), AgentTaskError>
where
    F: FnOnce(mpsc::Sender<AgentRuntimeRequest>, Channels) -> Fut,
    Fut: Future,
{
    let (runtime_tx, runtime_rx) = oneshot::channel();
    let factory = TestAgentFactory::new("agent".to_string(), sender, runtime_tx);
    let def = FfiAgentDef::new(TestRuntimeContext, spec, factory);
    let agent_context = TestAgentContext::default();
    let channels = agent_context.channels();

    let task_handle = async move {
        def.run(
            RouteUri::from_str("agent").unwrap(),
            Default::default(),
            Default::default(),
            Box::new(agent_context),
        )
        .await
        .expect("Failed to initialise agent")
        .await
    };

    try_join(task_handle, async move {
        let runtime_channel = runtime_rx.await.expect("No runtime channel received");
        test(runtime_channel, channels).await;
        Ok(())
    })
    .await
    .map(|_| ())
}

#[tokio::test]
async fn lifecycle_events() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();

    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |_runtime_tx, channels| async move {
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::InitComplete)
            .await;

        vtable_rx.did_start(Ok(())).await;
        channels.drop_all().await;
        vtable_rx.did_stop(Ok(())).await;
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn schedule_task_once() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        runtime_tx
            .send(AgentRuntimeRequest::ScheduleTask {
                id_lsb: 0,
                id_msb: 0,
                schedule: ScheduleDef::Once {
                    after: Duration::from_nanos(1),
                },
            })
            .await
            .expect("Runtime dropped");

        vtable_rx.did_start(Ok(())).await;
        vtable_rx.sync(0, id, Ok(Vec::new())).await;
        vtable_rx.run_task(0, 0, Ok(())).await;

        channels.drop_all().await;

        vtable_rx.did_stop(Ok(())).await;
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn schedule_task_interval() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        let run_count = 13;

        runtime_tx
            .send(AgentRuntimeRequest::ScheduleTask {
                id_lsb: 0,
                id_msb: 0,
                schedule: ScheduleDef::Interval {
                    run_count: 13,
                    interval: Duration::from_nanos(1),
                },
            })
            .await
            .expect("Runtime dropped");

        vtable_rx.did_start(Ok(())).await;
        vtable_rx.sync(0, id, Ok(Vec::new())).await;

        for _ in 0..run_count {
            vtable_rx.run_task(0, 0, Ok(())).await;
        }

        channels.drop_all().await;

        vtable_rx.did_stop(Ok(())).await;
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn schedule_task_infinite() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        runtime_tx
            .send(AgentRuntimeRequest::ScheduleTask {
                id_lsb: 0,
                id_msb: 0,
                schedule: ScheduleDef::Infinite {
                    interval: Duration::from_nanos(1),
                },
            })
            .await
            .expect("Runtime dropped");

        vtable_rx.did_start(Ok(())).await;
        vtable_rx.sync(0, id, Ok(Vec::new())).await;

        for _ in 0..10 {
            vtable_rx.run_task(0, 0, Ok(())).await;
        }

        channels.drop_all().await;

        drain_vtable_tasks(&mut vtable_rx.rx, 0, 0, || Ok(())).await;
    })
    .await;

    assert!(result.is_ok());
}

async fn drain_vtable_tasks<R>(
    rx: &mut mpsc::Receiver<VTableRequest>,
    expected_msb: i64,
    expected_lsb: i64,
    result: R,
) where
    R: Fn() -> Result<(), AgentTaskError>,
{
    loop {
        match rx.recv().await {
            Some(VTableRequest::RunTask {
                id_msb,
                id_lsb,
                promise,
            }) => {
                assert_eq!(id_msb, expected_msb);
                assert_eq!(id_lsb, expected_lsb);
                promise.send(result()).expect("Channel closed unexpectedly")
            }
            Some(VTableRequest::DidStop { promise }) => {
                promise.send(Ok(())).expect("Channel closed unexpectedly");
                break;
            }
            Some(req) => {
                panic!("Unexpected request: {:?}", req)
            }
            None => {
                panic!("Channel closed early")
            }
        }
    }
}

#[tokio::test]
async fn mixed_tasks() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        runtime_tx
            .send(AgentRuntimeRequest::ScheduleTask {
                id_lsb: 0,
                id_msb: 0,
                schedule: ScheduleDef::Once {
                    after: Duration::from_nanos(1),
                },
            })
            .await
            .expect("Runtime dropped");

        runtime_tx
            .send(AgentRuntimeRequest::ScheduleTask {
                id_lsb: 1,
                id_msb: 1,
                schedule: ScheduleDef::Interval {
                    run_count: 3,
                    interval: Duration::from_millis(500),
                },
            })
            .await
            .expect("Runtime dropped");

        runtime_tx
            .send(AgentRuntimeRequest::ScheduleTask {
                id_lsb: 2,
                id_msb: 2,
                schedule: ScheduleDef::Infinite {
                    interval: Duration::from_secs(2),
                },
            })
            .await
            .expect("Runtime dropped");

        vtable_rx.did_start(Ok(())).await;
        vtable_rx.sync(0, id, Ok(Vec::new())).await;

        vtable_rx.run_task(0, 0, Ok(())).await;

        vtable_rx.run_task(1, 1, Ok(())).await;
        vtable_rx.run_task(1, 1, Ok(())).await;
        vtable_rx.run_task(1, 1, Ok(())).await;

        vtable_rx.run_task(2, 2, Ok(())).await;
        vtable_rx.run_task(2, 2, Ok(())).await;

        channels.drop_all().await;

        drain_vtable_tasks(&mut vtable_rx.rx, 2, 2, || Ok(())).await;
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn syncs() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, mut channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx.did_start(Ok(())).await;
        vtable_rx
            .sync(0, id, {
                Ok(encode_to_vec(
                    vec![
                        IdentifiedLaneResponse::new(0, LaneResponse::SyncEvent(id, b"13")),
                        IdentifiedLaneResponse::new(0, LaneResponse::Synced(id)),
                    ],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_sync_event(&mut channels, "lane", id, BytesMut::from("13")).await;
        expect_value_synced(&mut channels, "lane", id).await;

        channels.drop_all().await;
        vtable_rx.did_stop(Ok(())).await;
        drop(runtime_tx);
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn dispatches() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, mut channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx.did_start(Ok(())).await;
        vtable_rx
            .sync(0, id, {
                Ok(encode_to_vec(
                    vec![
                        IdentifiedLaneResponse::new(0, LaneResponse::SyncEvent(id, b"13")),
                        IdentifiedLaneResponse::new(0, LaneResponse::Synced(id)),
                    ],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_sync_event(&mut channels, "lane", id, BytesMut::from("13")).await;
        expect_value_synced(&mut channels, "lane", id).await;

        channels
            .send(
                "lane".to_string(),
                LaneRequest::<BytesMut>::Command(BytesMut::from("12345")),
            )
            .await;

        vtable_rx
            .dispatch(0, BytesMut::from("12345"), {
                Ok(encode_to_vec(
                    vec![IdentifiedLaneResponse::new(
                        0,
                        LaneResponse::StandardEvent(Vec::from("54321")),
                    )],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_event(&mut channels, "lane", BytesMut::from("54321")).await;

        channels.drop_all().await;
        vtable_rx.did_stop(Ok(())).await;
        drop(runtime_tx);
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn flushes() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, mut channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx.did_start(Ok(())).await;
        vtable_rx
            .sync(0, id, {
                Ok(encode_to_vec(
                    vec![
                        IdentifiedLaneResponse::new(0, LaneResponse::SyncEvent(id, b"13")),
                        IdentifiedLaneResponse::new(0, LaneResponse::Synced(id)),
                    ],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_sync_event(&mut channels, "lane", id, BytesMut::from("13")).await;
        expect_value_synced(&mut channels, "lane", id).await;

        channels
            .send(
                "lane".to_string(),
                LaneRequest::<BytesMut>::Command(BytesMut::from("12345")),
            )
            .await;

        vtable_rx
            .dispatch(0, BytesMut::from("12345"), {
                Ok(encode_to_vec(
                    vec![
                        IdentifiedLaneResponse::new(
                            0,
                            LaneResponse::StandardEvent(Vec::from("54321")),
                        ),
                        IdentifiedLaneResponse::new(
                            0,
                            LaneResponse::StandardEvent(Vec::from("98765")),
                        ),
                    ],
                    DispatchEncoder::new(DispatchStatus::Incomplete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_event(&mut channels, "lane", BytesMut::from("54321")).await;
        expect_event(&mut channels, "lane", BytesMut::from("98765")).await;

        vtable_rx
            .flush_state({
                Ok(encode_to_vec(
                    vec![IdentifiedLaneResponse::new(
                        0,
                        LaneResponse::StandardEvent(Vec::from("45678")),
                    )],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_event(&mut channels, "lane", BytesMut::from("45678")).await;

        channels.drop_all().await;
        vtable_rx.did_stop(Ok(())).await;
        drop(runtime_tx);
    })
    .await;

    assert!(result.is_ok());
}

#[tokio::test]
async fn initializes_value_lane() {
    let (vtable_tx, mut vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(false, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, channels| async move {
        let mut buf = BytesMut::new();
        let mut store_encoder = StoreInitMessageEncoder::value();
        store_encoder
            .encode(StoreInitMessage::Command(b"13"), &mut buf)
            .expect("Encoding failed");
        store_encoder
            .encode(StoreInitMessage::<BytesMut>::InitComplete, &mut buf)
            .expect("Encoding failed");

        channels.send_bytes("lane".to_string(), buf.as_ref()).await;

        vtable_rx.init(0, BytesMut::from("13"), Ok(())).await;

        channels
            .send(
                "lane".to_string(),
                LaneRequest::<BytesMut>::Command(BytesMut::from("14")),
            )
            .await;

        vtable_rx.did_start(Ok(())).await;

        vtable_rx
            .dispatch(0, BytesMut::from("14"), {
                Ok(encode_to_vec(
                    vec![IdentifiedLaneResponse::new(
                        0,
                        LaneResponse::StandardEvent(b"13"),
                    )],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        channels.drop_all().await;
        vtable_rx.did_stop(Ok(())).await;
        drop(runtime_tx);
    })
    .await;
    assert!(result.is_ok());
}

#[derive(Debug, thiserror::Error)]
#[error("Error")]
struct Error;

async fn expect_error<F, Fut>(transient: bool, test: F)
where
    F: FnOnce(VTableChannelReceiver, Channels) -> Fut,
    Fut: Future,
{
    let (vtable_tx, vtable_rx) = vtable_channel();
    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(transient, 0, LaneKindRepr::Value),
        )]),
    );

    let result = run_agent(spec, vtable_tx, |runtime_tx, channels| async move {
        test(vtable_rx, channels).await;

        // The agent runtime is biased to processing runtime requests and therefore will ignore the
        // dispatch request and instead elect to shutdown the runtime. Since we're closing the
        // runtime via an error, we can ignore running the sender's drop implementation and instead
        // just shutdown the runtime with the error.
        forget(runtime_tx);
    })
    .await;

    match result {
        Ok(()) => {
            panic!("Expected a test failure")
        }
        Err(AgentTaskError::UserCodeError(e)) if e.downcast_ref::<Error>().is_some() => {}
        Err(e) => {
            panic!("Expected a user code error. Got: {:?}", e)
        }
    }
}

#[tokio::test]
async fn dispatch_error() {
    expect_error(true, |mut vtable_rx, mut channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx.did_start(Ok(())).await;
        vtable_rx
            .sync(0, id, {
                Ok(encode_to_vec(
                    vec![
                        IdentifiedLaneResponse::new(0, LaneResponse::SyncEvent(id, b"13")),
                        IdentifiedLaneResponse::new(0, LaneResponse::Synced(id)),
                    ],
                    DispatchEncoder::new(DispatchStatus::Complete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        expect_sync_event(&mut channels, "lane", id, BytesMut::from("13")).await;
        expect_value_synced(&mut channels, "lane", id).await;

        channels
            .send(
                "lane".to_string(),
                LaneRequest::<BytesMut>::Command(BytesMut::from("12345")),
            )
            .await;

        vtable_rx
            .dispatch(
                0,
                BytesMut::from("12345"),
                Err(AgentTaskError::UserCodeError(Box::new(Error))),
            )
            .await;
    })
    .await;
}

#[tokio::test]
async fn did_start_error() {
    expect_error(true, |mut vtable_rx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx
            .did_start(Err(AgentTaskError::UserCodeError(Box::new(Error))))
            .await;
    })
    .await;
}

#[tokio::test]
async fn did_stop_error() {
    expect_error(true, |mut vtable_rx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx.did_start(Ok(())).await;
        vtable_rx.sync(0, id, Ok(Vec::new())).await;

        channels.drop_all().await;
        vtable_rx
            .did_stop(Err(AgentTaskError::UserCodeError(Box::new(Error))))
            .await;
    })
    .await;
}

#[tokio::test]
async fn flush_error() {
    expect_error(true, |mut vtable_rx, channels| async move {
        let id = Uuid::from_u128(0);
        channels
            .send("lane".to_string(), LaneRequest::<BytesMut>::Sync(id))
            .await;

        vtable_rx.did_start(Ok(())).await;

        vtable_rx
            .sync(0, id, {
                Ok(encode_to_vec(
                    vec![IdentifiedLaneResponse::new(
                        0,
                        LaneResponse::SyncEvent(id, b"13"),
                    )],
                    DispatchEncoder::new(DispatchStatus::Incomplete, IdentifiedLaneResponseEncoder),
                ))
            })
            .await;

        vtable_rx
            .flush_state(Err(AgentTaskError::UserCodeError(Box::new(Error))))
            .await;
    })
    .await;
}

#[tokio::test]
async fn init_error() {
    expect_error(false, |mut vtable_rx, channels| async move {
        let mut buf = BytesMut::new();
        let mut store_encoder = StoreInitMessageEncoder::value();
        store_encoder
            .encode(StoreInitMessage::Command(b"13"), &mut buf)
            .expect("Encoding failed");
        store_encoder
            .encode(StoreInitMessage::<BytesMut>::InitComplete, &mut buf)
            .expect("Encoding failed");

        channels.send_bytes("lane".to_string(), buf.as_ref()).await;

        vtable_rx
            .init(
                0,
                BytesMut::from("13"),
                Err(AgentTaskError::UserCodeError(Box::new(Error))),
            )
            .await;
    })
    .await;
}
