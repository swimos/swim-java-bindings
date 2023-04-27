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

use std::num::NonZeroUsize;
use std::sync::Arc;

pub use client_runtime::ClientConfig;
use client_runtime::RemotePath;
use client_runtime::{
    start_runtime, DownlinkErrorKind, DownlinkRuntimeError, RawHandle, Transport,
};
use jni::objects::{GlobalRef, JValue};
use jni::JNIEnv;
use jni::JavaVM;
#[cfg(feature = "deflate")]
use ratchet::deflate::DeflateExtProvider;
#[cfg(not(feature = "deflate"))]
use ratchet::NoExtProvider;
use ratchet::WebSocketStream;
use swim_api::error::DownlinkTaskError;
use swim_runtime::net::dns::Resolver;
#[cfg(not(feature = "tls"))]
use swim_runtime::net::plain::TokioPlainTextNetworking;
use swim_runtime::net::ClientConnections;
use swim_runtime::ws::ext::RatchetNetworking;
use swim_runtime::ws::WsConnections;
#[cfg(feature = "tls")]
use swim_tls::{MaybeTlsStream, RustlsClientNetworking};
use swim_utilities::trigger;
use swim_utilities::trigger::promise;
#[cfg(not(feature = "tls"))]
use tokio::net::TcpStream;
use tokio::runtime::{Builder, Handle, Runtime};
use tokio::task::JoinHandle;

use jvm_sys::vm::method::{JavaObjectMethod, JavaObjectMethodDef};
use jvm_sys::vm::utils::{get_env_shared, get_env_shared_expect};
use jvm_sys::vm::{with_local_frame_null, SpannedError};
pub use macros::*;

use crate::downlink::value::{FfiValueDownlink, SharedVm};
use crate::downlink::{DownlinkConfigurations, ErrorHandlingConfig};

pub mod downlink;
mod macros;
include!(concat!(env!("OUT_DIR"), "/out.rs"));

pub struct SwimClient {
    error_mode: ErrorHandlingConfig,
    vm: SharedVm,
    runtime: Runtime,
    stop_tx: trigger::Sender,
    // todo: implement clone on RawHandle and remove this arc
    downlinks_handle: Arc<RawHandle>,
    _jh: JoinHandle<()>,
}

impl SwimClient {
    pub fn with_transport<Net, Ws>(
        runtime: Runtime,
        vm: JavaVM,
        transport: Transport<Net, Ws>,
        transport_buffer_size: NonZeroUsize,
        registration_buffer_size: NonZeroUsize,
        error_mode: ErrorHandlingConfig,
    ) -> SwimClient
    where
        Net: ClientConnections,
        Net::ClientSocket: WebSocketStream,
        Ws: WsConnections<Net::ClientSocket> + Sync + Send + 'static,
    {
        let (stop_tx, stop_rx) = trigger::trigger();
        let (handle, task) = start_runtime(
            registration_buffer_size,
            stop_rx,
            transport,
            transport_buffer_size,
        );

        let runtime_handle = runtime.handle();
        let _jh = {
            // The current Tokio runtime context needs to be set before the runtime task can be
            // spawned.
            let _guard = runtime_handle.enter();
            tokio::spawn(task)
        };

        SwimClient {
            error_mode,
            vm: Arc::new(vm),
            runtime,
            stop_tx,
            downlinks_handle: Arc::new(handle),
            _jh,
        }
    }

    pub fn new(vm: JavaVM, config: ClientConfig) -> SwimClient {
        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let ClientConfig {
            websocket,
            remote_buffer_size,
            transport_buffer_size,
            registration_buffer_size,
            ..
        } = config;
        let transport = Transport::new(
            build_networking(runtime.handle()),
            build_websockets(websocket),
            remote_buffer_size,
        );
        SwimClient::with_transport(
            runtime,
            vm,
            transport,
            transport_buffer_size,
            registration_buffer_size,
            ErrorHandlingConfig::Report,
        )
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

#[cfg(not(feature = "deflate"))]
fn build_websockets(config: client_runtime::WebSocketConfig) -> RatchetNetworking<NoExtProvider> {
    RatchetNetworking {
        config: ratchet::WebSocketConfig {
            max_message_size: config.max_message_size,
        },
        provider: NoExtProvider,
        subprotocols: Default::default(),
    }
}

#[cfg(feature = "deflate")]
fn build_websockets(
    config: client_runtime::WebSocketConfig,
) -> RatchetNetworking<DeflateExtProvider> {
    RatchetNetworking {
        config: ratchet::WebSocketConfig {
            max_message_size: config.max_message_size,
        },
        provider: DeflateExtProvider::with_config(config.deflate_config.unwrap_or_default()),
        subprotocols: Default::default(),
    }
}

// todo: tls
#[cfg(not(feature = "tls"))]
fn build_networking(runtime_handle: &Handle) -> impl ClientConnections<ClientSocket = TcpStream> {
    let resolver = runtime_handle.block_on(Resolver::new());
    TokioPlainTextNetworking::new(Arc::new(resolver))
}

#[cfg(feature = "tls")]
fn build_networking(
    runtime_handle: &Handle,
) -> impl ClientConnections<ClientSocket = MaybeTlsStream> {
    // todo: tls
    let resolver = runtime_handle.block_on(Resolver::new());
    RustlsClientNetworking::try_from_config(Arc::new(resolver), swim_tls::ClientConfig::new(vec![]))
        .unwrap()
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

    #[allow(clippy::result_unit_err)]
    pub fn spawn_value_downlink(
        &self,
        config: DownlinkConfigurations,
        downlink_ref: GlobalRef,
        stopped_barrier: GlobalRef,
        downlink: FfiValueDownlink,
        path: RemotePath,
    ) -> Result<(), ()> {
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
            path,
            runtime_config,
            downlink_config,
            options,
            downlink,
        ));

        let vm = vm.clone();
        match result {
            Ok(receiver) => {
                spawn_monitor(tokio_handle, vm, receiver, downlink_ref, stopped_barrier);
                Ok(())
            }
            Err(e) => {
                // any errors will have already been logged by the runtime
                let env = get_env_shared(&vm).expect("Failed to get JNI environment");
                env.throw_new(
                    "java/lang/Exception",
                    format!("Failed to spawn downlink: {:?}", e),
                )
                .expect("Failed to throw exception");
                Err(())
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
            .initialise(env)
            .expect("Failed to initialise countdown latch method");

    if let Err(e) = countdown.invoke(env, &stopped_barrier, &[]) {
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
