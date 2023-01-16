// Copyright 2015-2022 Swim Inc.
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

use crate::downlink::value::{FfiValueDownlink, SharedVm};
use crate::downlink::ErrorHandlingConfig;
use client_runtime::{start_runtime, RawHandle, Transport};
use jni::JavaVM;
use jvm_sys::vm::utils::get_env_shared;
use ratchet::{NoExtProvider, WebSocketConfig, WebSocketStream};
use std::num::NonZeroUsize;
use std::path::PathBuf;
use std::sync::Arc;
use swim_model::path::AbsolutePath;
use swim_runtime::downlink::DownlinkOptions;
use swim_runtime::remote::net::dns::Resolver;
use swim_runtime::remote::net::tls::TokioTlsNetworking;
use swim_runtime::remote::ExternalConnections;
use swim_runtime::ws::ext::RatchetNetworking;
use swim_runtime::ws::WsConnections;
use swim_utilities::trigger;
use tokio::runtime::{Builder, Handle, Runtime};
use url::Url;

pub mod downlink;

const REMOTE_BUFFER_SIZE: NonZeroUsize = unsafe { NonZeroUsize::new_unchecked(64) };

pub struct SwimClient {
    error_mode: ErrorHandlingConfig,
    vm: SharedVm,
    runtime: Runtime,
    stop_tx: trigger::Sender,
    downlinks_handle: Arc<RawHandle>,
}

impl SwimClient {
    pub fn with_transport<Net, Ws>(vm: JavaVM, transport: Transport<Net, Ws>) -> SwimClient
    where
        Net: ExternalConnections,
        Net::Socket: WebSocketStream,
        Ws: WsConnections<Net::Socket> + Sync,
    {
        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let (stop_tx, downlinks_handle) = runtime.block_on(async move {
            let (handle, stop_tx) = start_runtime(transport);
            (stop_tx, Arc::new(handle))
        });

        SwimClient {
            // todo: error mode constructor argument
            error_mode: ErrorHandlingConfig::Abort,
            vm: Arc::new(vm),
            runtime,
            stop_tx,
            downlinks_handle,
        }
    }

    pub fn new(vm: JavaVM) -> SwimClient {
        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let (stop_tx, downlinks_handle) = runtime.block_on(async move {
            let websockets = RatchetNetworking {
                config: WebSocketConfig::default(),
                provider: NoExtProvider,
                subprotocols: Default::default(),
            };
            let networking = TokioTlsNetworking::new::<_, Box<PathBuf>>(
                std::iter::empty(),
                Arc::new(Resolver::new().await),
            );
            let (handle, stop_tx) =
                start_runtime(Transport::new(networking, websockets, REMOTE_BUFFER_SIZE));

            (stop_tx, Arc::new(handle))
        });

        SwimClient {
            // todo: error mode constructor argument
            error_mode: ErrorHandlingConfig::Abort,
            vm: Arc::new(vm),
            runtime,
            stop_tx,
            downlinks_handle,
        }
    }

    pub fn handle(&self) -> ClientHandle {
        ClientHandle {
            error_mode: self.error_mode,
            vm: self.vm.clone(),
            tokio_handle: self.runtime.handle().clone(),
            downlinks_handle: self.downlinks_handle.clone(),
        }
    }

    pub fn shutdown(self) {
        self.stop_tx.trigger();
        self.runtime.shutdown_background();
    }
}

#[derive(Clone)]
pub struct ClientHandle {
    error_mode: ErrorHandlingConfig,
    vm: SharedVm,
    tokio_handle: Handle,
    downlinks_handle: Arc<RawHandle>,
}

impl ClientHandle {
    pub fn error_mode(&self) -> ErrorHandlingConfig {
        self.error_mode
    }
}

impl ClientHandle {
    pub fn vm(&self) -> SharedVm {
        self.vm.clone()
    }

    pub fn value_downlink(
        &self,
        downlink: FfiValueDownlink,
        host: Url,
        node: String,
        lane: String,
    ) {
        let ClientHandle {
            vm,
            tokio_handle,
            downlinks_handle,
            ..
        } = self;

        let result = tokio_handle.block_on(downlinks_handle.run_downlink(
            AbsolutePath::new(host, node.as_str(), lane.as_str()),
            Default::default(),
            Default::default(),
            DownlinkOptions::SYNC,
            downlink,
        ));

        match result {
            Ok(_) => {
                // any errors will have already been logged by the runtime
            }
            Err(e) => {
                let env = get_env_shared(vm).expect("Failed to get JNI environment");
                env.throw_new(
                    "java/lang/Exception",
                    format!("Failed to spawn downlink: {:?}", e),
                )
                .expect("Failed to throw exception");
            }
        }
    }
}
