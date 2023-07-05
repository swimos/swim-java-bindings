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

use jni::errors::Error;
use jni::sys::{ jint, jobject};
use jni::JNIEnv;
use jvm_sys::EnvExt;

use jvm_sys::vm::method::{JavaMethodExt, JavaObjectMethod};
use jvm_sys::vm::{jni_call, SpannedError};

use crate::downlink::vtable::{void_fn, JavaMethod,   ON_LINKED, ON_UNLINKED, ROUTINE_EXEC, DISPATCH_ON_UPDATE, DISPATCH_ON_REMOVE, DISPATCH_ON_CLEAR, DISPATCH_TAKE, DISPATCH_DROP};

pub struct MapDownlinkLifecycle {
    vtable: MapDownlinkVTable,
}

impl MapDownlinkLifecycle {
    pub fn from_parts(
        env: &JNIEnv,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
    ) -> Result<MapDownlinkLifecycle, Error> {
        Ok(MapDownlinkLifecycle {
            vtable: MapDownlinkVTable::new(
                env,
                on_linked,
                on_synced,
                on_update,
                on_remove,
                on_clear,
                on_unlinked,
                take,
                drop,
            )?,
        })
    }

    pub fn on_linked(&mut self, env: &JNIEnv) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        jni_call(env, || vtable.on_linked(env))
    }

    pub fn on_synced(&mut self, env: &JNIEnv) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        jni_call(env, || vtable.on_synced(env))
    }

    pub fn on_update(
        &mut self,
        env: &JNIEnv,
        mut key: Vec<u8>,
        mut value: Vec<u8>,
        dispatch: bool,
    ) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable } = self;
        jni_call(env, || {
            vtable.on_update(env, &mut key, &mut value, dispatch)
        })
    }

    pub fn on_remove(
        &mut self,
        env: &JNIEnv,
        mut key: Vec<u8>,
        dispatch: bool,
    ) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable } = self;
        jni_call(env, || {
            vtable.on_remove(env, &mut key, dispatch)
        })
    }

    pub fn on_clear(&mut self, env: &JNIEnv, dispatch: bool) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.on_clear(env, dispatch))
    }

    pub fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        jni_call(env, || vtable.on_unlinked(env))
    }

    pub fn take(&mut self, env: &JNIEnv, n: jint, dispatch: bool) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.take(env, n, dispatch))
    }

    pub fn drop(&mut self, env: &JNIEnv, n: jint, dispatch: bool) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.drop(env, n, dispatch))
    }
}

pub struct MapDownlinkVTable {
    on_linked: JavaMethod,
    on_synced: JavaMethod,
    on_update: JavaMethod,
    on_remove: JavaMethod,
    on_clear: JavaMethod,
    on_unlinked: JavaMethod,
    take: JavaMethod,
    drop: JavaMethod,
}

impl MapDownlinkVTable {
    fn new(
        env: &JNIEnv,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
    ) -> Result<MapDownlinkVTable, Error> {
        Ok(MapDownlinkVTable {
            on_linked: JavaMethod::for_method(&env, on_linked, ON_LINKED)?,
            on_synced: JavaMethod::for_method(&env, on_synced, ROUTINE_EXEC)?,
            on_update: JavaMethod::for_method(&env, on_update, DISPATCH_ON_UPDATE)?,
            on_remove: JavaMethod::for_method(&env, on_remove, DISPATCH_ON_REMOVE)?,
            on_clear: JavaMethod::for_method(&env, on_clear, DISPATCH_ON_CLEAR)?,
            on_unlinked: JavaMethod::for_method(&env, on_unlinked, ON_UNLINKED)?,
            take: JavaMethod::for_method(&env, take, DISPATCH_TAKE)?,
            drop: JavaMethod::for_method(&env, drop, DISPATCH_DROP)?,
        })
    }

    fn on_linked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_linked, &[])
    }

    fn on_synced(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_synced, &[])
    }

    fn on_update(
        &mut self,
        env: &JNIEnv,
        key: &mut Vec<u8>,
        value: &mut Vec<u8>,
        dispatch:bool,
    ) -> Result<(), Error> {
        let key = unsafe { env.new_direct_byte_buffer_exact(key)? };
        let value = unsafe { env.new_direct_byte_buffer_exact(value) }?;
        void_fn(
            env,
            &mut self.on_update,
            &[key.into(), value.into(), dispatch.into()],
        )
    }

    fn on_remove(
        &mut self,
        env: &JNIEnv,
        key: &mut Vec<u8>,
        dispatch:bool,
    ) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer_exact(key) }?;
        void_fn(env, &mut self.on_remove, &[buffer.into(), dispatch.into()])
    }

    fn on_clear(&mut self, env: &JNIEnv, dispatch: bool) -> Result<(), Error> {
        void_fn(env, &mut self.on_clear, &[dispatch.into()])
    }

    fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_unlinked, &[])
    }

    fn take(&mut self, env: &JNIEnv, n: jint, dispatch: bool) -> Result<(), Error> {
        self.take.execute(env, |init, obj| {
            init.void().invoke(env, obj, &[n.into(), dispatch.into()])
        })
    }

    fn drop(&mut self, env: &JNIEnv, n: jint, dispatch: bool) -> Result<(), Error> {
        self.drop.execute(env, |init, obj| {
            init.void().invoke(env, obj, &[n.into(), dispatch.into()])
        })
    }
}
