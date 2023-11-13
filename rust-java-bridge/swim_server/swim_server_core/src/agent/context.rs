use std::time::Duration;

use interval_stream::ScheduleDef;
use tokio::sync::mpsc;
use tracing::trace;
use uuid::Uuid;

use crate::agent::spec::LaneSpec;
use crate::agent::GuestRuntimeRequest;
use jvm_sys::env::JavaEnv;

pub struct JavaAgentContext {
    env: JavaEnv,
    tx: mpsc::Sender<GuestRuntimeRequest>,
}

impl JavaAgentContext {
    pub fn new(env: JavaEnv, tx: mpsc::Sender<GuestRuntimeRequest>) -> JavaAgentContext {
        JavaAgentContext { env, tx }
    }

    pub fn env(&self) -> JavaEnv {
        self.env.clone()
    }

    pub fn open_lane(&self, uri: String, spec: LaneSpec) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|_| {
            trace!(uri, spec = ?spec, "Java agent context opening lane");
            tx.blocking_send(GuestRuntimeRequest::OpenLane {
                uri: uri.into(),
                spec,
            })
        });
    }

    pub fn suspend_task(
        &self,
        resume_after_seconds: u64,
        resume_after_nanos: u32,
        id_msb: u64,
        id_lsb: u64,
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
        id_msb: u64,
        id_lsb: u64,
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
        count: usize,
        interval_seconds: u64,
        interval_nanos: u32,
        id_msb: u64,
        id_lsb: u64,
    ) {
        self.schedule(
            id_msb,
            id_lsb,
            ScheduleDef::Interval {
                count,
                interval: Duration::new(interval_seconds, interval_nanos),
            },
        )
    }

    fn schedule(&self, id_msb: u64, id_lsb: u64, schedule: ScheduleDef) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|_| {
            trace!(id_msb, id_lsb, schedule = ?schedule, "Java agent context scheduling task");
            tx.blocking_send(GuestRuntimeRequest::ScheduleTask {
                id: Uuid::from_u64_pair(id_msb, id_lsb),
                schedule,
            })
        });
    }

    pub fn cancel_task(&self, id_msb: u64, id_lsb: u64) {
        let JavaAgentContext { env, tx, .. } = self;
        env.with_env_expect(|_| {
            trace!(id_msb, id_lsb, "Java agent context cancelling task");
            tx.blocking_send(GuestRuntimeRequest::CancelTask {
                id: Uuid::from_u64_pair(id_msb, id_lsb),
            })
        });
    }
}
