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

use bytebridge::ByteCodec;
use bytes::BytesMut;
use client_runtime::RemotePath;
use jni::objects::{JObject, JString};
use jni::sys::{jbyteArray, jobject};
use jvm_sys::env::JavaEnv;
use jvm_sys::env::StringError;
use jvm_sys::{jni_try, npch};
use ratchet::WebSocketConfig;
use std::num::NonZeroUsize;
use std::panic;
use std::str::FromStr;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, ClientHandle, SwimClient};
use url::ParseError;
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
        let env = JavaEnv::new(env);

        let client = Box::leak(Box::new(SwimClient::new(
            env,
            config
        )));

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

        let handle = unsafe { &*handle };
        let env = handle.env();

        let mut config_bytes = env.with_env(|scope| {
            BytesMut::from_iter(scope.convert_byte_array(config))
        });
        let config = match env.with_env_throw("ai/swim/client/SwimClientException",|_| {
            DownlinkConfigurations::try_from_bytes(&mut config_bytes).map_err(StringError)
        }) {
            Ok(config) => config,
            Err(_) => return,
        };

        let downlink = FfiValueDownlink::create(
            handle.env(),
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        );

        let (host,node,lane) = env.with_env_throw("ai/swim/client/SwimClientException",move  |scope| {
            let host = Url::from_str(scope.get_rust_string(host).as_str())?;
            let node = scope.get_rust_string(node);
            let lane = scope.get_rust_string(lane);
            Ok::<(Url,String,String),ParseError>((host,node,lane))
        }).unwrap();

        env.with_env(|scope| {
            handle.spawn_value_downlink(
                config,
                scope.new_global_ref(unsafe{JObject::from_raw(downlink_ref)}),
                scope.new_global_ref(unsafe {JObject::from_raw(stopped_barrier)}),
                downlink,
                RemotePath::new(host.to_string(), node, lane)
            ).unwrap();
        });
    }
}
