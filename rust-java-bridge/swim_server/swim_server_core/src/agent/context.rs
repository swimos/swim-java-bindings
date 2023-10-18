use std::time::Duration;

use interval_stream::ScheduleDef;
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

    pub fn suspend_task(
        &self,
        resume_after_seconds: u64,
        resume_after_nanos: u32,
        id_msb: i64,
        id_lsb: i64,
    ) {
        self.schedule(
            id_msb,
            id_lsb,
            ScheduleDef::Once {
                after: Duration::new(resume_after_seconds, resume_after_nanos),
            },
        )
    }

    pub fn schedule_task_indefinitely(
        &self,
        interval_seconds: u64,
        interval_nanos: u32,
        id_msb: i64,
        id_lsb: i64,
    ) {
        self.schedule(
            id_msb,
            id_lsb,
            ScheduleDef::Infinite {
                interval: Duration::new(interval_seconds, interval_nanos),
            },
        )
    }

    pub fn repeat_task(
        &self,
        run_count: usize,
        interval_seconds: u64,
        interval_nanos: u32,
        id_msb: i64,
        id_lsb: i64,
    ) {
        self.schedule(
            id_msb,
            id_lsb,
            ScheduleDef::Interval {
                run_count,
                interval: Duration::new(interval_seconds, interval_nanos),
            },
        )
    }

    fn schedule(&self, id_msb: i64, id_lsb: i64, schedule: ScheduleDef) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|_| {
            tx.blocking_send(AgentRuntimeRequest::ScheduleTask {
                id_lsb,
                id_msb,
                schedule,
            })
        });
    }
}
