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

extern crate core;

use std::panic;

use bytes::BytesMut;
use client_runtime::{ClientConfig, RemotePath};
use jni::objects::JString;
use jni::sys::{jbyteArray, jobject};
use jni::JNIEnv;
use swim_api::downlink::Downlink;
use url::Url;

use jvm_sys::vm::set_panic_hook;
use jvm_sys::vm::utils::new_global_ref;
use jvm_sys::{jni_try, null_pointer_check_abort, parse_string};
use swim_client_core::downlink::map::FfiMapDownlink;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, ClientHandle, SwimClient};

client_fn! {
    SwimClient_startClient(
        env,
        _class,
    ) -> SwimClient {
        let client = Box::leak(Box::new(SwimClient::new(
            env.get_java_vm().expect("Failed to get Java VM"),
            ClientConfig::default()
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
