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
use crate::downlink::{DownlinkConfigurations, ErrorHandlingConfig};
use client_runtime::{
    start_runtime, DownlinkErrorKind, DownlinkRuntimeError, RawHandle, Transport,
};
use jni::objects::{GlobalRef, JValue};
use jni::JNIEnv;
use jni::JavaVM;
use jvm_sys::vm::method::{JavaObjectMethod, JavaObjectMethodDef};
use jvm_sys::vm::utils::{get_env_shared, get_env_shared_expect};
use jvm_sys::vm::{with_local_frame_null, SpannedError};
use ratchet::{NoExtProvider, WebSocketConfig, WebSocketStream};
use std::num::NonZeroUsize;
use std::path::PathBuf;
use std::sync::Arc;
use swim_api::error::DownlinkTaskError;
use swim_model::path::AbsolutePath;
use swim_runtime::remote::net::dns::Resolver;
use swim_runtime::remote::net::plain::TokioPlainTextNetworking;
use swim_runtime::remote::net::tls::TokioTlsNetworking;
use swim_runtime::remote::ExternalConnections;
use swim_runtime::ws::ext::RatchetNetworking;
use swim_runtime::ws::WsConnections;
use swim_utilities::trigger;
use swim_utilities::trigger::promise;
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
        Ws: WsConnections<Net::Socket> + Sync + Send + 'static,
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
            error_mode: ErrorHandlingConfig::Report,
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
            let networking = TokioPlainTextNetworking::new(Arc::new(Resolver::new().await));
            let (handle, stop_tx) =
                start_runtime(Transport::new(networking, websockets, REMOTE_BUFFER_SIZE));

            (stop_tx, Arc::new(handle))
        });

        SwimClient {
            // todo: error mode constructor argument
            error_mode: ErrorHandlingConfig::Report,
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

    pub fn tokio_handle(&self) -> Handle {
        self.tokio_handle.clone()
    }

    pub fn spawn_value_downlink(
        &self,
        config: DownlinkConfigurations,
        downlink_ref: GlobalRef,
        stopped_barrier: GlobalRef,
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

        let DownlinkConfigurations {
            runtime: runtime_config,
            downlink: downlink_config,
            options,
        } = config;
        let result = tokio_handle.block_on(downlinks_handle.run_downlink(
            AbsolutePath::new(host, node.as_str(), lane.as_str()),
            runtime_config,
            downlink_config,
            options,
            downlink,
        ));

        let vm = vm.clone();
        match result {
            Ok(receiver) => {
                spawn_monitor(tokio_handle, vm, receiver, downlink_ref, stopped_barrier);
            }
            Err(e) => {
                // any errors will have already been logged by the runtime
                let env = get_env_shared(&vm).expect("Failed to get JNI environment");
                env.throw_new(
                    "java/lang/Exception",
                    format!("Failed to spawn downlink: {:?}", e),
                )
                .expect("Failed to throw exception");
            }
        }
    }
}

fn spawn_monitor(
    tokio_handle: &Handle,
    vm: SharedVm,
    receiver: promise::Receiver<Result<(), DownlinkRuntimeError>>,
    downlink_ref: GlobalRef,
    stopped_barrier: GlobalRef,
) {
    tokio_handle.spawn(async move {
        let result = receiver
            .await
            .map_err(|_| DownlinkRuntimeError::new(DownlinkErrorKind::Terminated));
        let env = get_env_shared_expect(&vm);

        match result {
            Ok(arc_result) => {
                if let Err(e) = arc_result.as_ref() {
                    set_exception(&env, downlink_ref, e)
                }
            }
            Err(e) => set_exception(&env, downlink_ref, &e),
        }

        notify_stopped(&env, stopped_barrier);
    });
}

fn notify_stopped(env: &JNIEnv, stopped_barrier: GlobalRef) {
    let mut countdown =
        JavaObjectMethodDef::new("java/util/concurrent/CountDownLatch", "countDown", "()V")
            .initialise(&env)
            .expect("Failed to initialise countdown latch method");

    if let Err(e) = countdown.invoke(&env, &stopped_barrier, &[]) {
        env.fatal_error(&e.to_string());
    }
}

fn set_exception(env: &JNIEnv, downlink_ref: GlobalRef, cause: &DownlinkRuntimeError) {
    with_local_frame_null(env, None, || {
        let cause_message = match cause.downcast_ref::<DownlinkTaskError>() {
            Some(DownlinkTaskError::Custom(cause)) => match cause.downcast_ref::<SpannedError>() {
                Some(spanned) => {
                    env.set_field(
                        &downlink_ref,
                        "cause",
                        "Ljava/lang/Throwable;",
                        JValue::Object(spanned.cause_throwable.as_obj()),
                    )
                    .expect("Failed to report downlink exception cause");
                    spanned.cause.clone()
                }
                None => cause.to_string(),
            },
            _ => cause.to_string(),
        };

        let cause = env
            .new_string(cause_message)
            .expect("Failed to allocate java string");
        env.set_field(
            &downlink_ref,
            "message",
            "Ljava/lang/String;",
            JValue::Object(cause.into()),
        )
        .expect("Failed to report downlink exception message");
    });
}
