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

use jni::objects::JObject;
use jni::sys::jobject;
use tokio::runtime::{Builder, Runtime};

use jvm_sys::env::JavaEnv;
use jvm_sys::method::JavaMethodExt;
use jvm_sys::vtable::CountdownLatch;

/// Creates a new multi-threaded Tokio runtime and spawns 'fut' on to it. A monitor task is spawned
/// that waits for 'fut' to complete and then notifies 'barrier' that the task has completed; this
/// task must be run as its own Tokio task in case 'fut' panics as it is not possible to catch an
/// unwind due to uses of &mut T by readers and writers.
#[allow(clippy::not_unsafe_ptr_arg_deref)]
pub fn run_test<F>(env: JavaEnv, barrier: jobject, fut: F) -> *mut Runtime
where
    F: Future + Send + 'static,
    F::Output: Send + 'static,
{
    let global_ref =
        env.with_env(|scope| scope.new_global_ref(unsafe { JObject::from_raw(barrier) }));

    let runtime = Builder::new_multi_thread()
        .build()
        .expect("Failed to build runtime");

    let join_handle = runtime.spawn(fut);
    tokio::task::LocalSet::new();

    runtime.spawn(async move {
        let r = join_handle.await;

        env.with_env(|scope| {
            let _guard = scope.lock_obj(&global_ref);
            let method = scope.initialise(CountdownLatch::COUNTDOWN);

            scope.invoke(method.v(), &global_ref, &[]);
            if r.is_err() {
                scope.fatal_error("Test panicked");
            }
        });
    });

    Box::into_raw(Box::new(runtime))
}
