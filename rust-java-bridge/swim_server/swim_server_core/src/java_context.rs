// Copyright 2015-2024 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
