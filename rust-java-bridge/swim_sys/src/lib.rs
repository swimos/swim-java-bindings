// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::time::Duration;

use jni::objects::JClass;
use jni::JNIEnv;

use tokio::runtime::{Builder, Handle, Runtime};

use sys_util::{jvm_tryf, npch};

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_runtime_Runtime_startRuntime(
    env: JNIEnv,
    _class: JClass,
) -> *mut Runtime {
    let runtime = jvm_tryf!(env, Builder::new_multi_thread().enable_all().build());
    Box::leak(Box::new(runtime))
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "system" fn Java_ai_swim_bridge_runtime_Runtime_shutdownRuntime(
    env: JNIEnv,
    _class: JClass,
    runtime: *mut Runtime,
) {
    npch!(env, runtime);

    println!("Shutting down runtime");
    let runtime = Box::from_raw(runtime);
    runtime.shutdown_timeout(Duration::from_secs(10));
    println!("Shut down runtime");
}

#[no_mangle]
#[allow(clippy::missing_safety_doc)]
pub unsafe extern "system" fn Java_ai_swim_bridge_runtime_Runtime_newHandle(
    env: JNIEnv,
    _class: JClass,
    runtime: *mut Runtime,
) -> *mut Handle {
    npch!(env, runtime);

    let runtime = &*runtime;
    Box::leak(Box::new(runtime.handle().clone()))
}
