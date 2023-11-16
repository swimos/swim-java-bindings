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

use std::net::SocketAddr;
use std::num::NonZeroUsize;
use std::ptr::null_mut;
use std::str::FromStr;
use std::sync::Arc;

use bytes::BytesMut;
use client_runtime::{RemotePath, Transport};
use fixture::{MockClientConnections, MockWs, Server, WsAction};
use jni::objects::{JObject, JString};
use jni::sys::{jbyteArray, jobject};
use swim_form::Form;
use swim_model::{Blob, Text, Value};
use swim_utilities::non_zero_usize;
use tokio::io::duplex;
use tokio::runtime::Builder;
use tokio::runtime::Runtime;
use tokio::sync::Mutex;
use url::ParseError;
use url::Url;

use jvm_sys::env::JavaEnv;
use jvm_sys::jni_try;
use jvm_sys::method::JavaMethodExt;
use jvm_sys::vtable::Trigger;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, SwimClient};

use crate::lifecycle_test;

#[derive(Clone, Debug, PartialEq, Eq, Form)]
#[form_root(::swim_form)]
pub enum Notification {
    #[form(tag = "linked")]
    Linked {
        #[form(header)]
        node: Text,
        #[form(header)]
        lane: Text,
    },
    #[form(tag = "synced")]
    Synced {
        node: Text,
        lane: Text,
        #[form(body)]
        body: Option<Blob>,
    },
    #[form(tag = "unlinked")]
    Unlinked {
        #[form(header)]
        node: Text,
        #[form(header)]
        lane: Text,
    },
    #[form(tag = "event")]
    Event {
        node: Text,
        lane: Text,
        #[form(body)]
        body: Option<Value>,
    },
}

const DEFAULT_BUFFER_SIZE: NonZeroUsize = unsafe { NonZeroUsize::new_unchecked(64) };

client_fn! {
    pub fn downlink_value_ValueDownlinkTest_lifecycleTest(
        env,
        _class,
        lock: jobject,
        input: JString,
        host: JString,
        node: JString,
        lane: JString,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> Runtime {
        let env = JavaEnv::new(env);
        let downlink = FfiValueDownlink::create(
            env.clone(),
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        );
        lifecycle_test(downlink, env, lock, input, host, node, lane,true)
    }
}

fn create_io() -> (Transport<MockClientConnections, MockWs>, Server) {
    let (client_stream, server_stream) = duplex(128);
    let sock_addr: SocketAddr = "127.0.0.1:80".parse().unwrap();
    let ext = MockClientConnections::new(
        [(("127.0.0.1".to_string(), 80), sock_addr)],
        [(sock_addr, client_stream)],
    );
    let ws = MockWs::new([("127.0.0.1".to_string(), WsAction::Open)]);
    let transport = Transport::new(ext, ws, non_zero_usize!(128));
    let server = Server::new(server_stream);
    (transport, server)
}

client_fn! {
    pub fn downlink_value_ValueDownlinkTest_driveDownlinkError(
        env,
        _class,
        downlink_ref: JObject,
        stopped_barrier_ref: JObject,
        barrier: JObject,
        on_event: jobject,
    ) -> SwimClient {
        let env = JavaEnv::new(env);

        let (transport,  server) = create_io();
        let server = Arc::new(Mutex::new(server));

        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let client = SwimClient::with_transport(
            runtime,
            env.clone(),
            transport,
            DEFAULT_BUFFER_SIZE,
            DEFAULT_BUFFER_SIZE,
        );
        let handle = client.handle();
        let downlink = FfiValueDownlink::create(
            handle.env(),
            on_event,
            null_mut(),
            null_mut(),
            null_mut(),
            null_mut(),
        );

        let (stopped_barrier_gr,downlink_gr) = env.with_env(|scope| {
            (scope.new_global_ref(stopped_barrier_ref), scope.new_global_ref(downlink_ref))
        });

        jni_try! {
            handle.spawn_downlink(
                Default::default(),
                downlink_gr,
                stopped_barrier_gr,
                downlink,
                RemotePath::new("ws://127.0.0.1", "node", "lane")
            )
        };

        let async_runtime = handle.tokio_handle();
        let (trigger,barrier_global_ref) = env.with_env(|scope| {
           (scope.resolve(Trigger::TRIGGER), scope.new_global_ref(barrier))
        });

        let _jh = async_runtime.spawn(async move {
            let mut lane_peer = Server::lane_for(server, "node", "lane");
            lane_peer.await_link().await;
            lane_peer.await_sync(vec![13]).await;
            lane_peer.send_event(Value::text("blah")).await;

            env.with_env(|scope| {
                scope.invoke(trigger.v(), &barrier_global_ref, &[])
            });
        });

        Box::leak(Box::new(client))
    }
}

client_fn! {
    pub fn downlink_value_ValueDownlinkTest_parsesConfig(
        env,
        _class,
        config: jbyteArray,
    ) {
        let mut config_bytes = jni_try! {
            env,
            "Failed to parse configuration array",
            env.convert_byte_array(config).map(BytesMut::from_iter)
        };

        let _r = DownlinkConfigurations::try_from_bytes(&mut config_bytes);
    }
}

client_fn! {
    pub fn downlink_value_ValueDownlinkTest_driveDownlink(
        env,
        _class,
        downlink_ref: JObject,
        stopped_barrier_ref: JObject,
        barrier: JObject,
        host: JString,
        node: JString,
        lane: JString,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> SwimClient {
        let env = JavaEnv::new(env);
        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let (transport,  server) = create_io();
        let server = Arc::new(Mutex::new(server));

        let client = SwimClient::with_transport(
            runtime,
            env.clone(),
            transport,
            DEFAULT_BUFFER_SIZE,
            DEFAULT_BUFFER_SIZE,
        );
        let handle = client.handle();
        let downlink = FfiValueDownlink::create(
            env.clone(),
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        );

        let (host, node, lane) = match env.with_env_throw("ai/swim/client/SwimClientException", move |scope| {
            let host = Url::from_str(scope.get_rust_string(host).as_str())?;
            let node = scope.get_rust_string(node);
            let lane = scope.get_rust_string(lane);
            Ok::<(Url, String, String), ParseError>((host, node, lane))
        }) {
            Ok((host, node, lane)) => (host, node, lane),
            Err(()) => {
                return std::ptr::null_mut();
            }
        };

        let (stopped_barrier_gr,downlink_gr) = env.with_env(|scope| {
            (scope.new_global_ref(stopped_barrier_ref), scope.new_global_ref(downlink_ref))
        });

        jni_try! {
            handle.spawn_downlink(
                Default::default(),
                downlink_gr,
                stopped_barrier_gr,
                downlink,
                RemotePath::new(host.to_string(), node.clone(), lane.clone())
            )
        };

        let async_runtime = handle.tokio_handle();
        let barrier_global_ref = env.with_env(|scope| {
           scope.new_global_ref(barrier)
        });

        let jh = async_runtime.spawn(async move {
            let mut lane_peer = Server::lane_for(server, node, lane);
            lane_peer.await_link().await;
            lane_peer.await_sync(vec![13]).await;
            lane_peer.send_event(15).await;
            lane_peer.send_unlinked().await;

            env.with_env(|scope| {
                let method = scope.resolve(Trigger::TRIGGER);
                scope.invoke(method, &barrier_global_ref, &[]);
            });
        });
        std::mem::forget(jh);

        Box::leak(Box::new(client))
    }
}
