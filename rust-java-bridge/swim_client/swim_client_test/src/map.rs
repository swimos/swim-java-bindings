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

use crate::lifecycle_test;
use jni::objects::JString;
use jni::sys::jobject;
use jni::JNIEnv;
use jvm_sys::null_pointer_check_abort;
use jvm_sys::vm::set_panic_hook;
use std::fmt::Display;
use std::sync::Arc;
use swim_client_core::client_fn;
use swim_client_core::downlink::map::{FfiMapDownlink, MapDownlinkLifecycle};
use swim_client_core::downlink::ErrorHandlingConfig;
use tokio::runtime::Runtime;

client_fn! {
    downlink_map_MapDownlinkTest_callbackTest(
        env,
        _class,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
    ) {
        set_panic_hook();
        null_pointer_check_abort!(
            env,
            on_linked,
            on_synced,
            on_update,
            on_remove,
            on_clear,
            on_unlinked,
            take,
            drop
        );

        let mut vtable = MapDownlinkLifecycle::from_parts(
            &env,
            on_linked,
            on_synced,
            on_update,
            on_remove,
            on_clear,
            on_unlinked,
            take,
            drop
        ).unwrap();

        fn tri<F,O,E>(env:&JNIEnv, f:F) -> O
        where
            F: FnOnce() -> Result<O,E>,
            E: Display
        {
            match f() {
                Ok(o) => o,
                Err(e) => {
                    env.exception_describe().unwrap();
                    panic!("{}", e);
                }
            }
        }

        tri(&env, || vtable.on_linked(&env));
        tri(&env, || vtable.on_update(
            &env,
            b"key1".as_slice().to_vec(),
            b"value1".as_slice().to_vec(),
            false
        ));
        tri(&env, || vtable.on_update(
            &env,
            b"key2".as_slice().to_vec(),
            b"value2".as_slice().to_vec(),
            false
        ));
        tri(&env, || vtable.on_synced(&env));
        tri(&env, || vtable.on_remove(
            &env,
            b"key2".as_slice().to_vec(),
            true
        ));
        tri(&env, || vtable.on_clear(&env, true));
        tri(&env, || vtable.take(&env, 5, true));
        tri(&env, || vtable.drop(&env, 3, true));
        tri(&env, || vtable.on_unlinked(&env));
    }
}

client_fn! {
    downlink_map_MapDownlinkTest_lifecycleTest(
        env,
        _class,
        lock: jobject,
        input: JString,
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
    ) -> Runtime {
        set_panic_hook();

        let vm = Arc::new(env.get_java_vm().unwrap());
        let downlink = FfiMapDownlink::create(
            vm.clone(),
            on_linked,
            on_synced,
            on_update,
            on_remove,
            on_clear,
            on_unlinked,
            take,
            drop,
            ErrorHandlingConfig::Abort,
        ).unwrap();

        lifecycle_test(downlink, vm, lock, input, host, node, lane,false)
    }
}
