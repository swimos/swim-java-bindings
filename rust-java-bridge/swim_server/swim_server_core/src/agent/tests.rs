use std::collections::{HashMap, VecDeque};
use std::fmt::Debug;
use std::future::{ready, Future, Ready};
use std::str::FromStr;
use std::sync::Arc;

use bytes::BytesMut;
use futures_util::future::join;
use parking_lot::Mutex;
use swim_api::agent::Agent;
use swim_api::error::{AgentInitError, AgentTaskError};
use swim_api::protocol::agent::LaneRequest;
use swim_utilities::routing::route_uri::RouteUri;
use tokio::sync::{mpsc, oneshot};
use uuid::Uuid;

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
    expected_events: Arc<Mutex<VecDeque<VTableEvent>>>,
    runtime_provider: oneshot::Sender<mpsc::Sender<AgentRuntimeRequest>>,
}

struct TestAgentFactory {
    inner: Arc<Mutex<Option<TestAgentFactoryInner>>>,
}

impl TestAgentFactory {
    fn new(
        uri: String,
        expected_events: Arc<Mutex<VecDeque<VTableEvent>>>,
        runtime_provider: oneshot::Sender<mpsc::Sender<AgentRuntimeRequest>>,
    ) -> TestAgentFactory {
        TestAgentFactory {
            inner: Arc::new(Mutex::new(Some(TestAgentFactoryInner {
                uri,
                expected_events,
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
                expected_events,
                runtime_provider,
            }) => {
                if requested_uri == uri {
                    runtime_provider
                        .send(runtime_tx)
                        .expect("Failed to provide runtime channel");
                    Ok(TestAgentVTable { expected_events })
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

trait VTableProxy {
    type Args;
    type Response;

    fn run(self, args: Self::Args) -> Self::Response;
}

impl<V> VTableProxy for Box<V>
where
    V: VTableProxy,
{
    type Args = V::Args;
    type Response = V::Response;

    fn run(self, args: Self::Args) -> Self::Response {
        (*self).run(args)
    }
}

#[derive(Debug)]
struct DispatchProxy {
    args: (i32, BytesMut),
    response: Vec<u8>,
}

impl DispatchProxy {
    fn new(lane_id: i32, payload: BytesMut, response: Vec<u8>) -> DispatchProxy {
        DispatchProxy {
            args: (lane_id, payload),
            response,
        }
    }
}

impl VTableProxy for DispatchProxy {
    type Args = (i32, BytesMut);
    type Response = Result<Vec<u8>, AgentTaskError>;

    fn run(self, args: Self::Args) -> Self::Response {
        if self.args == args {
            Ok(self.response)
        } else {
            panic!("Expected: {:?}, received: {:?}", self.args, args)
        }
    }
}

#[derive(Debug)]
struct SyncProxy {
    args: (i32, Uuid),
    response: Vec<u8>,
}

impl SyncProxy {
    fn new(lane_id: i32, remote: Uuid, response: Vec<u8>) -> SyncProxy {
        SyncProxy {
            args: (lane_id, remote),
            response,
        }
    }
}

impl VTableProxy for SyncProxy {
    type Args = (i32, Uuid);
    type Response = Result<Vec<u8>, AgentTaskError>;

    fn run(self, args: Self::Args) -> Self::Response {
        if self.args == args {
            Ok(self.response)
        } else {
            panic!("Expected: {:?}, received: {:?}", self.args, args)
        }
    }
}

#[derive(Debug)]
struct InitProxy {
    args: (i32, BytesMut),
}

impl InitProxy {
    fn new(lane_id: i32, payload: BytesMut) -> InitProxy {
        InitProxy {
            args: (lane_id, payload),
        }
    }
}

impl VTableProxy for InitProxy {
    type Args = (i32, BytesMut);
    type Response = Result<(), AgentTaskError>;

    fn run(self, args: Self::Args) -> Self::Response {
        if self.args == args {
            Ok(())
        } else {
            panic!("Expected: {:?}, received: {:?}", self.args, args)
        }
    }
}

#[derive(Debug)]
struct FlushStateProxy {
    response: Vec<u8>,
}

impl FlushStateProxy {
    fn new(response: Vec<u8>) -> FlushStateProxy {
        FlushStateProxy { response }
    }
}

impl VTableProxy for FlushStateProxy {
    type Args = ();
    type Response = Result<Vec<u8>, AgentTaskError>;

    fn run(self, _args: Self::Args) -> Self::Response {
        Ok(self.response)
    }
}

#[derive(Debug)]
struct RunTaskProxy {
    args: (i64, i64),
}

impl RunTaskProxy {
    fn new(id_msb: i64, id_lsb: i64) -> RunTaskProxy {
        RunTaskProxy {
            args: (id_msb, id_lsb),
        }
    }
}

impl VTableProxy for RunTaskProxy {
    type Args = (i64, i64);
    type Response = Result<(), AgentTaskError>;

    fn run(self, _args: Self::Args) -> Self::Response {
        Ok(())
    }
}

#[derive(Debug)]
enum VTableEvent {
    DidStart,
    DidStop,
    Dispatch(DispatchProxy),
    Sync(SyncProxy),
    Init(InitProxy),
    FlushState(FlushStateProxy),
    RunTask(RunTaskProxy),
}

#[derive(Default)]
struct VTableEventDeque {
    deque: VecDeque<VTableEvent>,
}

impl VTableEventDeque {
    fn add_did_start(&mut self) -> &mut VTableEventDeque {
        self.deque.push_back(VTableEvent::DidStart);
        self
    }

    fn add_did_stop(&mut self) -> &mut VTableEventDeque {
        self.deque.push_back(VTableEvent::DidStop);
        self
    }

    fn add_dispatch(
        &mut self,
        lane_id: i32,
        payload: BytesMut,
        response: Vec<u8>,
    ) -> &mut VTableEventDeque {
        self.deque
            .push_back(VTableEvent::Dispatch(DispatchProxy::new(
                lane_id, payload, response,
            )));
        self
    }

    fn add_sync(&mut self, lane_id: i32, remote: Uuid, response: Vec<u8>) -> &mut VTableEventDeque {
        self.deque
            .push_back(VTableEvent::Sync(SyncProxy::new(lane_id, remote, response)));
        self
    }

    fn add_init(&mut self, lane_id: i32, payload: BytesMut) -> &mut VTableEventDeque {
        self.deque
            .push_back(VTableEvent::Init(InitProxy::new(lane_id, payload)));
        self
    }

    fn add_flush_state(&mut self, response: Vec<u8>) -> &mut VTableEventDeque {
        self.deque
            .push_back(VTableEvent::FlushState(FlushStateProxy::new(response)));
        self
    }

    fn add_run_task(&mut self, id_msb: i64, id_lsb: i64) -> &mut VTableEventDeque {
        self.deque
            .push_back(VTableEvent::RunTask(RunTaskProxy::new(id_msb, id_lsb)));
        self
    }
}

struct TestAgentVTable {
    expected_events: Arc<Mutex<VecDeque<VTableEvent>>>,
}

impl AgentVTable for TestAgentVTable {
    type Suspended<O> = Ready<Result<O, AgentTaskError>>
    where
        O: Send;

    fn did_start(&self) -> Self::Suspended<()> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::DidStart) => ready(Ok(())),
            o => {
                panic!("Expected a did start event. Got: {:?}", o)
            }
        }
    }

    fn did_stop(&self) -> Self::Suspended<()> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::DidStop) => ready(Ok(())),
            o => {
                panic!("Expected a did stop event. Got: {:?}", o)
            }
        }
    }

    fn dispatch(&mut self, lane_id: i32, buffer: BytesMut) -> Self::Suspended<Vec<u8>> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::Dispatch(proxy)) => ready(proxy.run((lane_id, buffer))),
            o => {
                panic!("Expected a dispatch stop event. Got: {:?}", o)
            }
        }
    }

    fn sync(&self, lane_id: i32, remote: Uuid) -> Self::Suspended<Vec<u8>> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::Sync(proxy)) => ready(proxy.run((lane_id, remote))),
            o => {
                panic!("Expected a sync event. Got: {:?}", o)
            }
        }
    }

    fn init(&mut self, lane_id: i32, msg: BytesMut) -> Self::Suspended<()> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::Init(proxy)) => ready(proxy.run((lane_id, msg))),
            o => {
                panic!("Expected an init event. Got: {:?}", o)
            }
        }
    }

    fn flush_state(&self) -> Self::Suspended<Vec<u8>> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::FlushState(proxy)) => ready(proxy.run(())),
            o => {
                panic!("Expected a flush state event. Got: {:?}", o)
            }
        }
    }

    fn run_task(&self, id_msb: i64, id_lsb: i64) -> Self::Suspended<()> {
        let expected = &mut *self.expected_events.lock();
        match expected.pop_front() {
            Some(VTableEvent::RunTask(proxy)) => ready(proxy.run((id_msb, id_lsb))),
            o => {
                panic!("Expected a run task event. Got: {:?}", o)
            }
        }
    }
}

async fn run_agent<F, Fut>(
    spec: AgentSpec,
    shared_events: Arc<Mutex<VecDeque<VTableEvent>>>,
    test: F,
) where
    F: FnOnce(mpsc::Sender<AgentRuntimeRequest>, Channels) -> Fut,
    Fut: Future,
{
    let (runtime_tx, runtime_rx) = oneshot::channel();
    let factory = TestAgentFactory::new("agent".to_string(), shared_events, runtime_tx);
    let def = FfiAgentDef::new(TestRuntimeContext, spec, factory);
    let agent_context = TestAgentContext::default();
    let channels = agent_context.channels();

    let agent_task = tokio::spawn(async move {
        def.run(
            RouteUri::from_str("agent").unwrap(),
            Default::default(),
            Default::default(),
            Box::new(agent_context),
        )
        .await
        .expect("Failed to initialise agent")
        .await
    });

    let runtime_channel = runtime_rx.await.expect("No runtime channel received");

    join(agent_task, test(runtime_channel, channels)).await;
}

#[tokio::test]
async fn lifecycle_events() {
    let mut expected = VTableEventDeque::default();
    expected.add_did_start().add_did_stop();

    let spec = AgentSpec::new(
        "agent".to_string(),
        HashMap::from([(
            "lane".to_string(),
            LaneSpec::new(true, 0, LaneKindRepr::Value),
        )]),
    );

    let expected_events = Arc::new(Mutex::new(expected.deque));

    run_agent(
        spec,
        expected_events.clone(),
        |runtime_tx, channels| async move {
            channels
                .send("lane".to_string(), LaneRequest::<BytesMut>::InitComplete)
                .await;
            channels.drop_all().await;
        },
    )
    .await;

    let expected = &mut *expected_events.lock();
    assert!(expected.is_empty());
}
