// Copyright 2015-2024 Swim Inc.
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

use jni::objects::JString;
use jni::sys::jobject;
use tokio::runtime::Runtime;

use jvm_sys::env::JavaEnv;
use jvm_sys::null_pointer_check_abort;
use swim_client_core::client_fn;
use swim_client_core::downlink::map::{FfiMapDownlink, MapDownlinkLifecycle};

use crate::lifecycle_test;

client_fn! {
    fn downlink_map_MapDownlinkTest_callbackTest(
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

        let env = JavaEnv::new(env);

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
        );

        let _r =env.with_env_throw("java/lang/RuntimeException", |_| {
            vtable.on_linked(&env)?;
            vtable.on_update(
                &env,
                &mut b"key1".as_slice().to_vec(),
               &mut  b"value1".as_slice().to_vec(),
                false
            )?;
            vtable.on_update(
                &env,
                &mut b"key2".as_slice().to_vec(),
               &mut  b"value2".as_slice().to_vec(),
                false
            )?;
            vtable.on_synced(&env)?;
            vtable.on_remove(
                &env,
               &mut  b"key2".as_slice().to_vec(),
                true
            )?;
            vtable.on_clear(&env, true)?;
            vtable.take(&env, 5, true)?;
            vtable.drop(&env, 3, true)?;
            vtable.on_unlinked(&env)
        });
    }
}

client_fn! {
    fn downlink_map_MapDownlinkTest_lifecycleTest(
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
        let env = JavaEnv::new(env);
        let downlink = FfiMapDownlink::create(
            env.clone(),
            on_linked,
            on_synced,
            on_update,
            on_remove,
            on_clear,
            on_unlinked,
            take,
            drop,
        );

        lifecycle_test(downlink, env, lock, input, host, node, lane,false)
    }
}
