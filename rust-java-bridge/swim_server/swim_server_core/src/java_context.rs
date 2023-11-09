use crate::agent::AgentRuntimeRequest;
use crate::spec::LaneSpec;
use jvm_sys::env::JavaEnv;
use tokio::runtime::Handle;
use tokio::sync::mpsc;

pub struct JavaAgentContext {
    env: JavaEnv,
    handle: Handle,
    tx: mpsc::Sender<AgentRuntimeRequest>,
}

impl JavaAgentContext {
    pub fn new(
        env: JavaEnv,
        handle: Handle,
        tx: mpsc::Sender<AgentRuntimeRequest>,
    ) -> JavaAgentContext {
        JavaAgentContext { env, handle, tx }
    }

    pub fn env(&self) -> JavaEnv {
        self.env.clone()
    }

    pub fn open_lane(&self, uri: String, spec: LaneSpec) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|scope| {
            tx.blocking_send(AgentRuntimeRequest::OpenLane {
                uri: uri.into(),
                spec,
            })
        });
    }
}
