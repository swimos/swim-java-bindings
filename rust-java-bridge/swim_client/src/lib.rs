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

use bytes::BytesMut;
use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jobject};
use jni::JNIEnv;
use std::panic;
use url::Url;

use jvm_sys::vm::set_panic_hook;
use jvm_sys::vm::utils::new_global_ref;
use jvm_sys::{jni_try, npch, parse_string};
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{ClientHandle, SwimClient};

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_SwimClient_startClient(
    env: JNIEnv,
    _class: JClass,
) -> *mut SwimClient {
    let client = Box::leak(Box::new(SwimClient::new(
        env.get_java_vm().expect("Failed to get Java VM"),
    )));

    set_panic_hook();

    client
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_SwimClient_shutdownClient(
    env: JNIEnv,
    _class: JClass,
    client: *mut SwimClient,
) {
    npch!(env, client);
    let runtime = unsafe { Box::from_raw(client) };
    runtime.shutdown();

    let _hook = panic::take_hook();
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_Handle_createHandle(
    env: JNIEnv,
    _class: JClass,
    runtime: *mut SwimClient,
) -> *mut ClientHandle {
    npch!(env, runtime);
    let runtime = unsafe { &*runtime };
    let handle = runtime.handle();

    Box::leak(Box::new(handle))
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_Handle_dropHandle(
    env: JNIEnv,
    _class: JClass,
    handle: *mut ClientHandle,
) {
    npch!(env, handle);
    unsafe {
        drop(Box::from_raw(handle));
    }
}

// If the number of arguments for this grows any further then it might be worth implementing an FFI
// builder pattern that finalises with an 'open' call which this accepts and takes ownership of.
#[no_mangle]
pub extern "system" fn Java_ai_swim_client_downlink_value_ValueDownlinkModel_open(
    env: JNIEnv,
    _class: JClass,
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

    handle.spawn_value_downlink(
        config,
        make_global_ref(downlink_ref, "downlink object"),
        make_global_ref(stopped_barrier, "stopped barrier"),
        downlink,
        host,
        node,
        lane,
    );
}
