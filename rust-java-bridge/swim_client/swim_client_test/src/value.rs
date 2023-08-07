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
use std::net::SocketAddr;
use std::num::NonZeroUsize;
use std::ptr::null_mut;

use bytes::BytesMut;
use client_runtime::{RemotePath, Transport};
use fixture::{MockClientConnections, MockWs, Server, WsAction};
use futures_util::future::try_join3;
use futures_util::SinkExt;
use jni::objects::{JObject, JString};
use jni::sys::{jbyteArray, jobject};
use swim_api::downlink::{Downlink, DownlinkConfig};
use swim_api::protocol::downlink::{DownlinkNotification, DownlinkNotificationEncoder};
use swim_form::Form;
use swim_model::address::Address;
use swim_model::{Blob, Text, Value};
use swim_recon::parser::{parse_recognize, Span};
use swim_recon::printer::print_recon_compact;
use swim_utilities::io::byte_channel::byte_channel;
use swim_utilities::non_zero_usize;
use tokio::io::{duplex, AsyncReadExt};
use tokio::runtime::Builder;
use tokio::runtime::Runtime;
use tokio_util::codec::FramedWrite;
use url::ParseError;
use url::Url;

use jvm_sys::env::JavaEnv;
use jvm_sys::env::StringError;
use jvm_sys::jni_try;
use jvm_sys::method::JavaMethodExt;
use jvm_sys::vtable::CountdownLatch;
use jvm_sys_tests::run_test;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, SwimClient};

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
        let env = JavaEnv::new(env);
        let downlink = FfiValueDownlink::create(
            env.clone(),
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        );

        let (host,node,lane,input) = env.with_env(|scope| {
            let host = scope.get_rust_string(host);
            let node = scope.get_rust_string(node);
            let lane = scope.get_rust_string(lane);
            let input = scope.get_rust_string(input);

            (host,node,lane,input)
        });

        let (input_tx, input_rx) = byte_channel(non_zero_usize!(128));
        let (output_tx, mut output_rx) = byte_channel(non_zero_usize!(128));

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
                Some(host.into()),
                node.into(),
                lane.into(),
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
        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let (transport, mut server) = create_io();
        let env = JavaEnv::new(env);

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
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        );

        let parse_result = env.with_env_throw("ai/swim/client/SwimClientException", |scope| {
            let host = Url::try_from(scope.get_rust_string(host).as_str())?;
            let node = scope.get_rust_string(node);
            let lane = scope.get_rust_string(lane);

            Ok::<_, ParseError>((host,node,lane))
        });

        match parse_result {
            Ok((host,node,lane)) => {
                let spawn_result = env.with_env(|scope| {
                   handle.spawn_value_downlink(
                        Default::default(),
                        scope.new_global_ref(unsafe { JObject::from_raw(downlink_ref) }),
                        scope.new_global_ref(unsafe { JObject::from_raw( stopped_barrier_ref) }),
                        downlink,
                        RemotePath::new(host.to_string(), node.clone(), lane.clone())
                    )
                });

                match spawn_result {
                    Ok(()) => {
                        env.with_env(|scope| {
                            let async_runtime = handle.tokio_handle();
                            let barrier_global_ref = scope
                                .new_global_ref(unsafe { JObject::from_raw(barrier) });
                            let countdown= scope.initialise(CountdownLatch::COUNTDOWN);
                            let scoped_env = env.clone();

                            let handle = async_runtime.spawn(async move {
                                let mut lane_peer = server.lane_for(node, lane);
                                lane_peer.await_link().await;
                                lane_peer.await_sync(13).await;
                                lane_peer.send_event(15).await;
                                lane_peer.send_unlinked().await;

                                scoped_env.with_env(|local_scope| {
                                    local_scope.invoke(countdown.v(),&barrier_global_ref, &[]);
                                });
                            });

                            std::mem::forget(handle);
                        });

                        Box::leak(Box::new(client))
                    },
                    Err(()) => {
                        std::ptr::null_mut()
                    }
                }
            },
            Err(_) => {
                std::ptr::null_mut()
            }
        }
    }
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
        downlink_ref: JObject,
        stopped_barrier_ref: JObject,
        barrier: JObject,
        on_event: jobject,
    ) -> SwimClient {
        let env = JavaEnv::new(env);
        let runtime = Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("Failed to build Tokio runtime");

        let (transport, mut server) = create_io();
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

        let spawn_result = env.with_env(|scope| {
            let downlink_gr = scope.new_global_ref(downlink_ref);
            let stopped_barrier_gr = scope.new_global_ref(stopped_barrier_ref);

            handle.spawn_value_downlink(
                Default::default(),
                downlink_gr,
                stopped_barrier_gr,
                downlink,
                RemotePath::new("ws://127.0.0.1", "node", "lane")
            ).map(|_| {
                scope.new_global_ref(barrier)
            })
        });

        match spawn_result {
            Ok(barrier_gr) => {
                let async_runtime = handle.tokio_handle();
                let handle = async_runtime.spawn(async move {
                    let countdown = env.initialise(CountdownLatch::COUNTDOWN);
                    let mut lane_peer = server.lane_for("node", "lane");

                    lane_peer.await_link().await;
                    lane_peer.await_sync(13).await;
                    lane_peer.send_event(Value::text("blah")).await;

                    env.with_env(|scope|{
                        scope.invoke(countdown.v(), &barrier_gr, &[])
                    });
                });
                std::mem::forget(handle);
                Box::leak(Box::new(client))
            },
            Err(()) => {
                std::ptr::null_mut()
            }
        }
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

        let env = JavaEnv::new(env);
        let _r = env.with_env_throw("ai/swim/client/SwimClientException", |_| {
           DownlinkConfigurations::try_from_bytes(&mut config_bytes).map_err(StringError)
        });
    }
}
