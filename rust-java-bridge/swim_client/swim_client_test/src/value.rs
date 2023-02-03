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

use std::io::ErrorKind;
use std::ptr::null_mut;
use std::sync::Arc;

use bytes::BytesMut;
use client_runtime::Transport;
use fixture::{MockExternalConnections, MockWs, Server, WsAction};
use futures_util::future::try_join3;
use futures_util::SinkExt;
use jni::errors::Error;
use jni::objects::{GlobalRef, JObject, JString};
use jni::sys::{jbyteArray, jobject};
use jni::JNIEnv;
use swim_api::downlink::{Downlink, DownlinkConfig};
use swim_api::protocol::downlink::{DownlinkNotification, DownlinkNotificationEncoder};
use swim_form::Form;
use swim_model::address::Address;
use swim_model::{Blob, Text, Value};
use swim_recon::parser::{parse_recognize, Span};
use swim_recon::printer::print_recon_compact;
use swim_runtime::net::{Scheme, SchemeHostPort, SchemeSocketAddr};
use swim_utilities::io::byte_channel::byte_channel;
use swim_utilities::non_zero_usize;
use tokio::io::{duplex, AsyncReadExt};
use tokio::runtime::Runtime;
use tokio_util::codec::FramedWrite;
use url::Url;

use jvm_sys::vm::method::{JavaObjectMethod, JavaObjectMethodDef};
use jvm_sys::vm::set_panic_hook;
use jvm_sys::vm::utils::{get_env_shared, new_global_ref};
use jvm_sys::{ffi_fn, jni_try, jvm_tryf, parse_string};
use jvm_sys_tests::run_test;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::downlink::ErrorHandlingConfig;
use swim_client_core::{client_fn, SwimClient};

#[derive(Clone, Debug, PartialEq, Form)]
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

        let host = env.get_string(host).unwrap();
        let node = env.get_string(node).unwrap();
        let lane = env.get_string(lane).unwrap();

        let (input_tx, input_rx) = byte_channel(non_zero_usize!(128));
        let (output_tx, mut output_rx) = byte_channel(non_zero_usize!(128));
        let input = env.get_string(input).unwrap().to_str().unwrap().to_string();

        let write_task = async move {
            let mut framed = FramedWrite::new(input_tx, DownlinkNotificationEncoder);
            for notif in input.lines() {
                match parse_recognize::<Notification>(Span::new(notif), false).unwrap() {
                    Notification::Linked { .. } => framed
                        .send(DownlinkNotification::<Blob>::Linked)
                        .await
                        .expect("Failed to encode message"),
                    Notification::Synced { .. } => framed
                        .send(DownlinkNotification::<Blob>::Synced)
                        .await
                        .expect("Failed to encode message"),
                    Notification::Unlinked { .. } => framed
                        .send(DownlinkNotification::<Blob>::Unlinked)
                        .await
                        .expect("Failed to encode message"),
                    Notification::Event { body, .. } => framed
                        .send(DownlinkNotification::Event {
                            body: format!("{}", print_recon_compact(&body)).into_bytes(),
                        })
                        .await
                        .expect("Failed to encode message"),
                }
            }

            Ok(())
        };

        let read_task = async move {
            let mut buf = BytesMut::new();
            match output_rx.read(&mut buf).await {
                Ok(0) => {}
                Ok(_) => {
                    panic!("Unexpected downlink value read: {:?}", buf.as_ref());
                }
                Err(e) => {
                    if e.kind() != ErrorKind::BrokenPipe {
                        panic!("Downlink read channel error: {:?}", e);
                    }
                }
            }

            Ok(())
        };

        let downlink_task = downlink.run(
            Address::new(
                Some(host.to_str().unwrap().into()),
                node.to_str().unwrap().into(),
                lane.to_str().unwrap().into(),
            ),
            DownlinkConfig {
                events_when_not_synced: true,
                terminate_on_unlinked: false,
                buffer_size: non_zero_usize!(1024),
            },
            input_rx,
            output_tx,
        );

        let task = async move {
            try_join3(write_task, read_task, downlink_task)
                .await
                .expect("Downlink task failure");
        };

        run_test(env, lock, task)
    }
}

client_fn! {
    downlink_value_ValueDownlinkTest_dropRuntime(
        _env,
        _class,
        ptr: *mut Runtime,
    ) {
        unsafe {
            drop(Box::from_raw(ptr));
        }
    }
}

fn create_io() -> (Transport<MockExternalConnections, MockWs>, Server) {
    let (client_stream, server_stream) = duplex(128);
    let ext = MockExternalConnections::new(
        [(
            SchemeHostPort::new(Scheme::Ws, "127.0.0.1".to_string(), 80),
            SchemeSocketAddr::new(Scheme::Ws, "127.0.0.1:80".parse().unwrap()),
        )],
        [("127.0.0.1:80".parse().unwrap(), client_stream)],
    );
    let ws = MockWs::new([("127.0.0.1".to_string(), WsAction::Open)]);
    let transport = Transport::new(ext, ws, non_zero_usize!(128));
    let server = Server::new(server_stream);
    (transport, server)
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

        let (transport, mut server) = create_io();

        let client =
            SwimClient::with_transport(env.get_java_vm().expect("Failed to get Java VM"), transport);
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
            handle.spawn_value_downlink(
                Default::default(),
                make_global_ref(&env, downlink_ref, "downlink object reference"),
                make_global_ref(&env, stopped_barrier_ref, "stopped barrier"),
                downlink,
                host.clone(),
                node.clone(),
                lane.clone(),
            )
        };

        let async_runtime = handle.tokio_handle();
        let barrier_global_ref = env
            .new_global_ref(unsafe { JObject::from_raw(barrier) })
            .unwrap();
        let vm = handle.vm();

        let mut countdown =
            JavaObjectMethodDef::new("java/util/concurrent/CountDownLatch", "countDown", "()V")
                .initialise(&env)
                .unwrap();

        let mut countdown_latch =
            move |env: &JNIEnv, global_ref| match countdown.invoke(&env, &global_ref, &[]) {
                Ok(_) => {}
                Err(Error::JavaException) => {
                    let throwable = env.exception_occurred().unwrap();
                    jvm_tryf!(env, env.throw(throwable));
                }
                Err(e) => env.fatal_error(&e.to_string()),
            };

        let _jh = async_runtime.spawn(async move {
            let mut lane_peer = server.lane_for(node, lane);
            lane_peer.await_link().await;
            lane_peer.await_sync(13).await;
            lane_peer.send_event(15).await;
            lane_peer.send_unlinked().await;

            let env = get_env_shared(&vm).unwrap();
            countdown_latch(&env, barrier_global_ref);
        });

        Box::leak(Box::new(client))
    }
}

fn make_global_ref(env: &JNIEnv, obj: jobject, name: &str) -> GlobalRef {
    new_global_ref(&env, obj)
        .expect(&format!(
            "Failed to create new global reference for {}",
            name
        ))
        .unwrap()
}

client_fn! {
    downlink_value_ValueDownlinkTest_dropSwimClient(
        _env,
        _class,
        ptr: *mut SwimClient,
    ) {
        unsafe {
            drop(Box::from_raw(ptr));
        }
    }
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

        let (transport, mut server) = create_io();
        let client =
            SwimClient::with_transport(env.get_java_vm().expect("Failed to get Java VM"), transport);
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
            handle.spawn_value_downlink(
                Default::default(),
                make_global_ref(&env, downlink_ref, "downlink object reference"),
                make_global_ref(&env, stopped_barrier_ref, "stopped barrier"),
                downlink,
                "ws://127.0.0.1".parse().unwrap(),
                "node".to_string(),
                "lane".to_string(),
            )
        };

        let async_runtime = handle.tokio_handle();
        let barrier_global_ref = env
            .new_global_ref(unsafe { JObject::from_raw(barrier) })
            .unwrap();
        let vm = handle.vm();

        let mut countdown =
            JavaObjectMethodDef::new("java/util/concurrent/CountDownLatch", "countDown", "()V")
                .initialise(&env)
                .unwrap();

        let _jh = async_runtime.spawn(async move {
            let mut lane_peer = server.lane_for("node", "lane");
            lane_peer.await_link().await;
            lane_peer.await_sync(13).await;
            lane_peer.send_event(Value::text("blah")).await;
            let env = get_env_shared(&vm).unwrap();

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
    downlink_value_ValueDownlinkTest_parsesConfig(
        env,
        _class,
        config: jbyteArray,
    ) {
        let mut config_bytes = jni_try! {
            env,
            "Failed to parse configuration array",
            env.convert_byte_array(config).map(BytesMut::from_iter)
        };

        let _r = DownlinkConfigurations::try_from_bytes(&mut config_bytes, &env);
    }
}
