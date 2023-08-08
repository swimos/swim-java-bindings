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

use std::future::Future;

use jni::errors::Error;
use jni::objects::JObject;
use jni::sys::jobject;
use jni::JNIEnv;
use tokio::runtime::{Builder, Runtime};

use jvm_sys::vm::method::{JavaObjectMethod, JavaObjectMethodDef};
use jvm_sys::vm::utils::VmExt;
use jvm_sys::{jvm_tryf, null_pointer_check_abort};

/// Creates a new multi-threaded Tokio runtime and spawns 'fut' on to it. A monitor task is spawned
/// that waits for 'fut' to complete and then notifies 'barrier' that the task has completed; this
/// task must be run as its own Tokio task in case 'fut' panics as it is not possible to catch an
/// unwind due to uses of &mut T by readers and writers.
#[allow(clippy::not_unsafe_ptr_arg_deref)]
pub fn run_test<F>(env: JNIEnv, barrier: jobject, fut: F) -> *mut Runtime
where
    F: Future + Send + 'static,
    F::Output: Send + 'static,
{
    null_pointer_check_abort!(env, barrier);

    let vm = env.get_java_vm().unwrap();
    let global_ref = env
        .new_global_ref(unsafe { JObject::from_raw(barrier) })
        .unwrap();

    let runtime = Builder::new_multi_thread()
        .build()
        .expect("Failed to build runtime");

    let join_handle = runtime.spawn(fut);

    runtime.spawn(async move {
        let r = join_handle.await;
        let env = vm.env_or_abort();

        let _guard = env.lock_obj(&global_ref).expect("Failed to enter monitor");
        let countdown = JavaObjectMethodDef::new("ai/swim/concurrent/Trigger", "trigger", "()V")
            .initialise(&env)
            .unwrap();

        match countdown.invoke(&env, &global_ref, &[]) {
            Ok(_) => {}
            Err(Error::JavaException) => {
                let throwable = env.exception_occurred().unwrap();
                jvm_tryf!(env, env.throw(throwable));
            }
            Err(e) => env.fatal_error(&e.to_string()),
        }
        if r.is_err() {
            env.fatal_error("Test panicked");
        }
    });

    Box::into_raw(Box::new(runtime))
}
