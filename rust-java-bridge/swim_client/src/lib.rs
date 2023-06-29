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
use jvm_sys::{jni_try, npch, parse_string};
use ratchet::WebSocketConfig;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, ClientHandle, SwimClient};
use url::Url;

include!(concat!(env!("OUT_DIR"), "/out.rs"));

impl From<ClientConfig> for swim_client_core::ClientConfig {
    fn from(config: ClientConfig) -> Self {
        let ClientConfig {
            max_message_size,
            remote_buffer_size,
            transport_buffer_size,
            registration_buffer_size,
            #[cfg(feature = "deflate")]
            server_max_window_bits,
            #[cfg(feature = "deflate")]
            client_max_window_bits,
            #[cfg(feature = "deflate")]
            request_server_no_context_takeover,
            #[cfg(feature = "deflate")]
            request_client_no_context_takeover,
            #[cfg(feature = "deflate")]
            accept_no_context_takeover,
            #[cfg(feature = "deflate")]
            compression_level,
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
                        server_max_window_bits,
                    )
                    .unwrap(),
                    client_max_window_bits: ratchet_deflate::WindowBits::try_from(
                        client_max_window_bits,
                    )
                    .unwrap(),
                    request_server_no_context_takeover,
                    request_client_no_context_takeover,
                    accept_no_context_takeover,
                    compression_level: Compression::new(compression_level),
                }
            },
            remote_buffer_size: into_non_zero(remote_buffer_size),
            transport_buffer_size: into_non_zero(transport_buffer_size),
            registration_buffer_size: into_non_zero(registration_buffer_size),
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
        npch!(env, client);
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
        npch!(env, runtime);
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
        npch!(env, handle);
        unsafe {
            drop(Box::from_raw(handle));
        }
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
        npch!(env, handle, stopped_barrier, downlink_ref, config);

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

        let make_global_ref = |obj, name| {
            // this closure is only called with arguments that have already had a null check
            // performed, so the unwrap is safe
            new_global_ref(&env, obj)
                .unwrap_or_else(|_| panic!(
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
            handle.spawn_value_downlink(
                config,
                make_global_ref(downlink_ref, "downlink object"),
                make_global_ref(stopped_barrier, "stopped barrier"),
                downlink,
                RemotePath::new(host.to_string(), node, lane)
            ),
        };
    }
}
