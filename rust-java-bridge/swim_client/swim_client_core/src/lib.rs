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

use crate::downlink::DownlinkConfigurations;
use client_runtime::{
    start_runtime, DownlinkErrorKind, DownlinkRuntimeError, RawHandle, Transport,
};
pub use client_runtime::{ClientConfig, RemotePath};
use jni::objects::{GlobalRef, JValue};
use jvm_sys::env::{JavaEnv, SpannedError};
#[cfg(feature = "deflate")]
use ratchet::deflate::{DeflateConfig, DeflateExtProvider};
#[cfg(not(feature = "deflate"))]
use ratchet::NoExtProvider;
use ratchet::WebSocketStream;
use std::num::NonZeroUsize;
use std::sync::Arc;
use swim_api::downlink::Downlink;
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

use jvm_sys::method::JavaMethodExt;
use jvm_sys::vtable::Trigger;
pub use macros::*;

pub mod downlink;
mod macros;
pub use macros::*;

pub struct SwimClient {
    env: JavaEnv,
    runtime: Runtime,
    stop_tx: trigger::Sender,
    // todo: implement clone on RawHandle and remove this arc
    downlinks_handle: Arc<RawHandle>,
    _jh: JoinHandle<()>,
}

impl SwimClient {
    pub fn with_transport<Net, Ws>(
        runtime: Runtime,
        env: JavaEnv,
        transport: Transport<Net, Ws>,
        transport_buffer_size: NonZeroUsize,
        registration_buffer_size: NonZeroUsize,
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
            false,
        );

        let runtime_handle = runtime.handle();
        let _jh = {
            // The current Tokio runtime context needs to be set before the runtime task can be
            // spawned.
            let _guard = runtime_handle.enter();
            tokio::spawn(task)
        };

        SwimClient {
            env,
            runtime,
            stop_tx,
            downlinks_handle: Arc::new(handle),
            _jh,
        }
    }

    pub fn new(env: JavaEnv, config: ClientConfig) -> SwimClient {
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

        #[cfg(feature = "deflate")]
        let websockets = build_websockets(
            ratchet::WebSocketConfig {
                max_message_size: websocket.max_message_size,
            },
            websocket.deflate_config,
        );
        #[cfg(not(feature = "deflate"))]
        let websockets = build_websockets(ratchet::WebSocketConfig {
            max_message_size: websocket.max_message_size,
        });

        let transport = Transport::new(
            build_networking(runtime.handle()),
            websockets,
            remote_buffer_size,
        );
        SwimClient::with_transport(
            runtime,
            env,
            transport,
            transport_buffer_size,
            registration_buffer_size,
        )
    }

    pub fn handle(&self) -> ClientHandle {
        ClientHandle {
            env: self.env.clone(),
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
fn build_websockets(config: ratchet::WebSocketConfig) -> RatchetNetworking<NoExtProvider> {
    RatchetNetworking {
        config,
        provider: NoExtProvider,
        subprotocols: Default::default(),
    }
}

#[cfg(feature = "deflate")]
fn build_websockets(
    config: ratchet::WebSocketConfig,
    deflate_config: Option<DeflateConfig>,
) -> RatchetNetworking<DeflateExtProvider> {
    RatchetNetworking {
        config,
        provider: DeflateExtProvider::with_config(deflate_config.unwrap_or_default()),
        subprotocols: Default::default(),
    }
}

#[cfg(not(feature = "tls"))]
fn build_networking(runtime_handle: &Handle) -> impl ClientConnections<ClientSocket = TcpStream> {
    let resolver = runtime_handle.block_on(Resolver::new());
    TokioPlainTextNetworking::new(Arc::new(resolver))
}

#[cfg(feature = "tls")]
fn build_networking(
    runtime_handle: &Handle,
) -> impl ClientConnections<ClientSocket = MaybeTlsStream> {
    let resolver = runtime_handle.block_on(Resolver::new());
    RustlsClientNetworking::try_from_config(Arc::new(resolver), swim_tls::ClientConfig::new(vec![]))
        .unwrap()
}

#[derive(Clone)]
pub struct ClientHandle {
    env: JavaEnv,
    tokio_handle: Handle,
    downlinks_handle: Arc<RawHandle>,
}

impl ClientHandle {
    pub fn env(&self) -> JavaEnv {
        self.env.clone()
    }

    pub fn tokio_handle(&self) -> Handle {
        self.tokio_handle.clone()
    }

    #[allow(clippy::result_unit_err)]
    pub fn spawn_downlink<D>(
        &self,
        config: DownlinkConfigurations,
        downlink_ref: GlobalRef,
        stopped_barrier: GlobalRef,
        downlink: D,
        path: RemotePath,
    ) -> Result<(), ()>
    where
        D: Downlink + Send + Sync + 'static,
    {
        let ClientHandle {
            env,
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

        let env = env.clone();
        match result {
            Ok(receiver) => {
                spawn_monitor(tokio_handle, env, receiver, downlink_ref, stopped_barrier);
                Ok(())
            }
            Err(e) => {
                // any errors will have already been logged by the runtime
                env.with_env(|scope| {
                    scope.throw_new(
                        "java/lang/Exception",
                        format!("Failed to spawn downlink: {:?}", e),
                    );
                });
                Err(())
            }
        }
    }
}

fn spawn_monitor(
    tokio_handle: &Handle,
    env: JavaEnv,
    receiver: promise::Receiver<Result<(), DownlinkRuntimeError>>,
    downlink_ref: GlobalRef,
    stopped_barrier: GlobalRef,
) {
    tokio_handle.spawn(async move {
        let result = receiver
            .await
            .map_err(|_| DownlinkRuntimeError::new(DownlinkErrorKind::Terminated));
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

fn notify_stopped(env: &JavaEnv, stopped_barrier: GlobalRef) {
    env.with_env(|scope| {
        let countdown = scope.resolve(Trigger::TRIGGER);
        scope.invoke(countdown.v(), &stopped_barrier, &[]);
    });
}

fn set_exception(env: &JavaEnv, downlink_ref: GlobalRef, cause: &DownlinkRuntimeError) {
    env.with_env(|scope| {
        let cause_message = match cause.downcast_ref::<DownlinkTaskError>() {
            Some(DownlinkTaskError::Custom(cause)) => match cause.downcast_ref::<SpannedError>() {
                Some(spanned) => {
                    scope.set_field(
                        &downlink_ref,
                        "cause",
                        "Ljava/lang/Throwable;",
                        JValue::Object(spanned.throwable.as_obj()),
                    );
                    spanned.error.clone()
                }
                None => cause.to_string(),
            },
            _ => cause.to_string(),
        };

        let cause = scope.new_string(cause_message);
        scope.set_field(
            &downlink_ref,
            "message",
            "Ljava/lang/String;",
            JValue::Object(cause.into()),
        );
    });
}
