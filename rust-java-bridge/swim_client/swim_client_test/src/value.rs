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
use std::sync::Arc;

use client_runtime::{RemotePath, Transport};
use fixture::{MockClientConnections, MockWs, Server, WsAction};
use jni::errors::Error;
use jni::objects::{GlobalRef, JObject, JString};
use jni::sys::jobject;
use jni::JNIEnv;
use swim_form::Form;
use swim_model::{Blob, Text, Value};
use swim_utilities::non_zero_usize;
use tokio::io::duplex;
use tokio::runtime::Builder;
use tokio::runtime::Runtime;
use tokio::sync::Mutex;
use url::Url;

use jvm_sys::vm::method::{JavaObjectMethod, JavaObjectMethodDef};
use jvm_sys::vm::set_panic_hook;
use jvm_sys::vm::utils::new_global_ref;
use jvm_sys::vm::utils::VmExt;
use jvm_sys::{jni_try, jvm_tryf, parse_string};
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::ErrorHandlingConfig;
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
const DEFAULT_ERROR_MODE: ErrorHandlingConfig = ErrorHandlingConfig::Report;

client_fn! {
    downlink_value_ValueDownlinkTest_lifecycleTest(
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
        set_panic_hook();

        let vm = Arc::new(env.get_java_vm().unwrap());
        let downlink = FfiValueDownlink::create(
            vm.clone(),
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
            ErrorHandlingConfig::Abort,
        )
        .expect("Failed to build downlink");
        lifecycle_test(downlink, vm, lock, input, host, node, lane,true)
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
    downlink_value_ValueDownlinkTest_driveDownlinkError(
        env,
        _class,
        downlink_ref: jobject,
        stopped_barrier_ref: jobject,
        barrier: jobject,
        on_event: jobject,
    ) -> SwimClient {
        set_panic_hook();

        let (transport,  server) = create_io();
        let server = Arc::new(Mutex::new(server));

        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let client = SwimClient::with_transport(
            runtime,
            env.get_java_vm().expect("Failed to get Java VM"),
            transport,
            DEFAULT_BUFFER_SIZE,
            DEFAULT_BUFFER_SIZE,
            DEFAULT_ERROR_MODE
        );
        let handle = client.handle();
        let downlink = jni_try! {
            env,
            "Failed to create downlink",
            FfiValueDownlink::create(
                handle.vm(),
                on_event,
                null_mut(),
                null_mut(),
                null_mut(),
                null_mut(),
                handle.error_mode(),
            ),
            std::ptr::null_mut()
        };

        jni_try! {
            handle.spawn_downlink(
                Default::default(),
                make_global_ref(&env, downlink_ref, "downlink object reference"),
                make_global_ref(&env, stopped_barrier_ref, "stopped barrier"),
                downlink,
                RemotePath::new("ws://127.0.0.1", "node", "lane")
            )
        };

        let async_runtime = handle.tokio_handle();
        let barrier_global_ref = env
            .new_global_ref(unsafe { JObject::from_raw(barrier) })
            .unwrap();
        let vm = handle.vm();

        let  countdown =
            JavaObjectMethodDef::new("ai/swim/concurrent/Trigger", "trigger", "()V")
                .initialise(&env)
                .unwrap();

        let _jh = async_runtime.spawn(async move {
            let mut lane_peer = Server::lane_for(server, "node", "lane");
            lane_peer.await_link().await;
            lane_peer.await_sync(vec![13]).await;
            lane_peer.send_event(Value::text("blah")).await;
            let env = vm.expect_env();

            match countdown.invoke(&env, &barrier_global_ref, &[]) {
                Ok(_) => {}
                Err(Error::JavaException) => {
                    let throwable = env.exception_occurred().unwrap();
                    jvm_tryf!(env, env.throw(throwable));
                }
                Err(e) => env.fatal_error(&e.to_string()),
            }
        });

        Box::leak(Box::new(client))
    }
}

client_fn! {
    downlink_value_ValueDownlinkTest_driveDownlink(
        env,
        _class,
        downlink_ref: jobject,
        stopped_barrier_ref: jobject,
        barrier: jobject,
        host: JString,
        node: JString,
        lane: JString,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> SwimClient {
        set_panic_hook();

        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let (transport,  server) = create_io();
        let server = Arc::new(Mutex::new(server));

        let client = SwimClient::with_transport(
            runtime,
            env.get_java_vm().expect("Failed to get Java VM"),
                transport,
            DEFAULT_BUFFER_SIZE,
            DEFAULT_BUFFER_SIZE,
            DEFAULT_ERROR_MODE
        );
        let handle = client.handle();
        let downlink = jni_try! {
            env,
            "Failed to create downlink",
            FfiValueDownlink::create(
                handle.vm(),
                on_event,
                on_linked,
                on_set,
                on_synced,
                on_unlinked,
                handle.error_mode(),
            ),
            std::ptr::null_mut()
        };
        let host = jni_try! {
            env,
            "Failed to parse host URL",
            Url::try_from(parse_string!(env, host, std::ptr::null_mut()).as_str()),
            std::ptr::null_mut()
        };

        let node = parse_string!(env, node, std::ptr::null_mut());
        let lane = parse_string!(env, lane, std::ptr::null_mut());

        jni_try! {
            handle.spawn_downlink(
                Default::default(),
                make_global_ref(&env, downlink_ref, "downlink object reference"),
                make_global_ref(&env, stopped_barrier_ref, "stopped barrier"),
                downlink,
                RemotePath::new(host.to_string(), node.clone(), lane.clone())
            )
        };

        let async_runtime = handle.tokio_handle();
        let barrier_global_ref = env
            .new_global_ref(unsafe { JObject::from_raw(barrier) })
            .unwrap();
        let vm = handle.vm();

        let  countdown =
            JavaObjectMethodDef::new("ai/swim/concurrent/Trigger", "trigger", "()V")
                .initialise(&env)
                .unwrap();

        let  countdown_latch =
            move |env: &JNIEnv, global_ref| match countdown.invoke(env, &global_ref, &[]) {
                Ok(_) => {}
                Err(Error::JavaException) => {
                    let throwable = env.exception_occurred().unwrap();
                    jvm_tryf!(env, env.throw(throwable));
                }
                Err(e) => env.fatal_error(&e.to_string()),
            };

        let _jh = async_runtime.spawn(async move {
            let mut lane_peer = Server::lane_for(server, node, lane);
            lane_peer.await_link().await;
            lane_peer.await_sync(vec![13]).await;
            lane_peer.send_event(15).await;
            lane_peer.send_unlinked().await;

            let env = vm.expect_env();
            countdown_latch(&env, barrier_global_ref);
        });

        Box::leak(Box::new(client))
    }
}

fn make_global_ref(env: &JNIEnv, obj: jobject, name: &str) -> GlobalRef {
    new_global_ref(env, obj)
        .unwrap_or_else(|_| panic!("Failed to create new global reference for {}", name))
        .unwrap()
}
