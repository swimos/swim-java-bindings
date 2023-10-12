use std::time::Duration;

use tokio::sync::mpsc;

use crate::agent::spec::LaneSpec;
use crate::agent::AgentRuntimeRequest;
use jvm_sys::env::JavaEnv;

pub struct JavaAgentContext {
    env: JavaEnv,
    tx: mpsc::Sender<AgentRuntimeRequest>,
}

impl JavaAgentContext {
    pub fn new(env: JavaEnv, tx: mpsc::Sender<AgentRuntimeRequest>) -> JavaAgentContext {
        JavaAgentContext { env, tx }
    }

    pub fn env(&self) -> JavaEnv {
        self.env.clone()
    }

    pub fn open_lane(&self, uri: String, spec: LaneSpec) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|_| {
            tx.blocking_send(AgentRuntimeRequest::OpenLane {
                uri: uri.into(),
                spec,
            })
        });
    }

    pub fn suspend_task(&self, time_seconds: u64, time_nanos: u32, id_msb: i64, id_lsb: i64) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|_| {
            tx.blocking_send(AgentRuntimeRequest::ScheduleTask {
                id_lsb,
                id_msb,
                interval: Duration::new(time_seconds, time_nanos),
            })
        });
    }
}
