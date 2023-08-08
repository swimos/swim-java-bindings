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
use std::panic;

use bytebridge::ByteCodec;
use bytes::BytesMut;
use client_runtime::RemotePath;
use jni::objects::JString;
use jni::sys::{jbyteArray, jobject};
use jvm_sys::vm::set_panic_hook;
use jvm_sys::vm::utils::new_global_ref;
use jvm_sys::{jni_try, null_pointer_check_abort, parse_string};
use ratchet::WebSocketConfig;
use swim_client_core::downlink::map::FfiMapDownlink;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, ClientHandle, SwimClient};
use url::Url;

include!(concat!(env!("OUT_DIR"), "/out.rs"));

impl From<ClientConfig> for swim_client_core::ClientConfig {
    fn from(config: ClientConfig) -> Self {
        let ClientConfig {
            max_message_size,
            remote_buffer_size: _remote_buffer_size,
            transport_buffer_size: _transport_buffer_size,
            registration_buffer_size: _registration_buffer_size,
            server_max_window_bits: _server_max_window_bits,
            client_max_window_bits: _client_max_window_bits,
            request_server_no_context_takeover: _request_server_no_context_takeover,
            request_client_no_context_takeover: _request_client_no_context_takeover,
            accept_no_context_takeover: _accept_no_context_takeover,
            compression_level: _compression_level,
        } = config;

        let into_non_zero = |val: u32| unsafe { NonZeroUsize::new_unchecked(val as usize) };

        swim_client_core::ClientConfig {
            websocket: WebSocketConfig {
                max_message_size: max_message_size as usize,
            },
            #[cfg(feature = "deflate")]
            deflate: {
                use ratchet_deflate::{Compression, DeflateConfig, WindowBits};

                DeflateConfig {
                    server_max_window_bits: ratchet_deflate::WindowBits::try_from(
                        _server_max_window_bits,
                    )
                    .unwrap(),
                    client_max_window_bits: ratchet_deflate::WindowBits::try_from(
                        _client_max_window_bits,
                    )
                    .unwrap(),
                    request_server_no_context_takeover: _request_server_no_context_takeover,
                    request_client_no_context_takeover: _request_client_no_context_takeover,
                    accept_no_context_takeover: _accept_no_context_takeover,
                    compression_level: Compression::new(_compression_level),
                }
            },
            remote_buffer_size: into_non_zero(_remote_buffer_size),
            transport_buffer_size: into_non_zero(_transport_buffer_size),
            registration_buffer_size: into_non_zero(_registration_buffer_size),
        }
    }
}

client_fn! {
    SwimClient_startClient(
        env,
        _class,
        config: jbyteArray,
    ) -> SwimClient {
        let mut config_bytes = jni_try! {
            env,
            "Failed to parse configuration array",
            env.convert_byte_array(config).map(BytesMut::from_iter),
            std::ptr::null_mut()
        };
        let config = jni_try!{
            env,
            "Failed to read client configuration",
            ClientConfig::try_from_bytes(&mut config_bytes).map(Into::into),
            std::ptr::null_mut()
        };

        let client = Box::leak(Box::new(SwimClient::new(
            env.get_java_vm().expect("Failed to get Java VM"),
            config
        )));

        set_panic_hook();

        client
    }
}

client_fn! {
    SwimClient_shutdownClient(
        env,
        _class,
        client: *mut SwimClient,
    ) {
        null_pointer_check_abort!(env, client);
        let runtime = unsafe { Box::from_raw(client) };
        runtime.shutdown();

        let _hook = panic::take_hook();
    }
}

client_fn! {
    Handle_createHandle(
        env,
        _class,
        runtime: *mut SwimClient,
    ) -> ClientHandle {
        null_pointer_check_abort!(env, runtime);
        let runtime = unsafe { &*runtime };
        let handle = runtime.handle();

        Box::leak(Box::new(handle))
    }
}

client_fn! {
    Handle_dropHandle(
        env,
        _class,
        handle: *mut ClientHandle,
    ) {
        null_pointer_check_abort!(env, handle);
        unsafe {
            drop(Box::from_raw(handle));
        }
    }
}

/// Attempts to open a downlink using the provided client handle. This function assumes that the
/// downlink_ref, config, and stopped_barrier are not null pointers.
fn open_downlink<D>(
    env: JNIEnv,
    handle: &ClientHandle,
    downlink_ref: jobject,
    config: jbyteArray,
    stopped_barrier: jobject,
    host: JString,
    node: JString,
    lane: JString,
    downlink: D,
) where
    D: Downlink + Send + Sync + 'static,
{
    let mut config_bytes = jni_try! {
        env,
        "Failed to parse configuration array",
        env.convert_byte_array(config).map(BytesMut::from_iter)
    };

    let config = jni_try! {
        env,
        "Invalid config",
        DownlinkConfigurations::try_from_bytes(&mut config_bytes,&env)
    };

    let make_global_ref = |obj, name| {
        new_global_ref(&env, obj)
            .expect(&format!(
                "Failed to create new global reference for {}",
                name
            ))
            .unwrap()
    };

    let host = jni_try! {
        env,
        "Failed to parse host URL",
        Url::try_from(parse_string!(env, host).as_str())
    };

    let node = parse_string!(env, node);
    let lane = parse_string!(env, lane);

    jni_try! {
        handle.spawn_downlink(
            config,
            make_global_ref(downlink_ref, "downlink object"),
            make_global_ref(stopped_barrier, "stopped barrier"),
            downlink,
            RemotePath::new(host.to_string(),node,lane)
        ),
        ()
    }
}

client_fn! {
    // If the number of arguments for this grows any further then it might be worth implementing an
    // FFI builder pattern that finalises with an 'open' call which this accepts and takes ownership
    // of.
    downlink_value_ValueDownlinkModel_open(
        env,
        _class,
        handle: *mut ClientHandle,
        downlink_ref: jobject,
        config: jbyteArray,
        stopped_barrier: jobject,
        host: JString,
        node: JString,
        lane: JString,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) {
        null_pointer_check_abort!(env, handle, stopped_barrier, downlink_ref, config);

        let handle = unsafe { &*handle };
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
        };

        open_downlink(
            env,
            handle,
            downlink_ref,
            config,
            stopped_barrier,
            host,
            node,
            lane,
            downlink
        );
    }
}

client_fn! {
    downlink_map_MapDownlinkModel_open(
        env,
        _class,
        handle: *mut ClientHandle,
        downlink_ref: jobject,
        config: jbyteArray,
        stopped_barrier: jobject,
        host: JString,
        node: JString,
        lane: JString,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
    ) {
        null_pointer_check_abort!(env, handle, stopped_barrier, downlink_ref, config);

        let handle = unsafe { &*handle };
        let downlink = jni_try! {
            env,
            "Failed to create downlink",
            FfiMapDownlink::create(
                handle.vm(),
                on_linked,
                on_synced,
                on_update,
                on_remove,
                on_clear,
                on_unlinked,
                take,
                drop,
                handle.error_mode(),
            ),
        };

        open_downlink(
            env,
            handle,
            downlink_ref,
            config,
            stopped_barrier,
            host,
            node,
            lane,
            downlink
        );
    }
}
