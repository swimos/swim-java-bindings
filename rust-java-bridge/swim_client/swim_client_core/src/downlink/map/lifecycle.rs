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
use jni::objects::{GlobalRef, JValue};
use jni::sys::{jint, jobject};
use jni::JNIEnv;
use jvm_sys::EnvExt;

use jvm_sys::vm::method::{void_fn, JavaCallback, JavaMethodExt, JavaObjectMethod};
use jvm_sys::vm::{jni_call, SpannedError};

use crate::downlink::vtable::{
    BI_CONSUMER_ACCEPT, CONSUMER_ACCEPT, ON_LINKED, ON_UNLINKED, ROUTINE_EXEC, TRI_CONSUMER_ACCEPT,
};

struct BoolWrapper {
    bool_true: GlobalRef,
    bool_false: GlobalRef,
}

impl BoolWrapper {
    fn boolean_for(&self, b: bool) -> &GlobalRef {
        if b {
            &self.bool_true
        } else {
            &self.bool_false
        }
    }
}

pub struct MapDownlinkLifecycle {
    vtable: MapDownlinkVTable,
    wrapper: BoolWrapper,
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
        let true_field =
            env.get_static_field("java/lang/Boolean", "TRUE", "Ljava/lang/Boolean;")?;
        let bool_true = env.new_global_ref(true_field.l()?)?;

        let false_field =
            env.get_static_field("java/lang/Boolean", "FALSE", "Ljava/lang/Boolean;")?;
        let bool_false = env.new_global_ref(false_field.l()?)?;

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
            wrapper: BoolWrapper {
                bool_true,
                bool_false,
            },
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
        let MapDownlinkLifecycle { vtable, wrapper } = self;
        jni_call(env, || {
            vtable.on_update(env, &mut key, &mut value, wrapper.boolean_for(dispatch))
        })
    }

    pub fn on_remove(
        &mut self,
        env: &JNIEnv,
        mut key: Vec<u8>,
        dispatch: bool,
    ) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, wrapper } = self;
        jni_call(env, || {
            vtable.on_remove(env, &mut key, wrapper.boolean_for(dispatch))
        })
    }

    pub fn on_clear(&mut self, env: &JNIEnv, dispatch: bool) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, wrapper } = self;
        jni_call(env, || vtable.on_clear(env, wrapper.boolean_for(dispatch)))
    }

    pub fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        jni_call(env, || vtable.on_unlinked(env))
    }

    pub fn take(&mut self, env: &JNIEnv, n: jint, dispatch: bool) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, wrapper } = self;
        jni_call(env, || vtable.take(env, n, wrapper.boolean_for(dispatch)))
    }

    pub fn drop(&mut self, env: &JNIEnv, n: jint, dispatch: bool) -> Result<(), SpannedError> {
        let MapDownlinkLifecycle { vtable, wrapper } = self;
        jni_call(env, || vtable.drop(env, n, wrapper.boolean_for(dispatch)))
    }
}

pub struct MapDownlinkVTable {
    on_linked: JavaCallback,
    on_synced: JavaCallback,
    on_update: JavaCallback,
    on_remove: JavaCallback,
    on_clear: JavaCallback,
    on_unlinked: JavaCallback,
    take: JavaCallback,
    drop: JavaCallback,
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
            on_linked: JavaCallback::for_method(&env, on_linked, ON_LINKED)?,
            on_synced: JavaCallback::for_method(&env, on_synced, ROUTINE_EXEC)?,
            on_update: JavaCallback::for_method(&env, on_update, TRI_CONSUMER_ACCEPT)?,
            on_remove: JavaCallback::for_method(&env, on_remove, BI_CONSUMER_ACCEPT)?,
            on_clear: JavaCallback::for_method(&env, on_clear, CONSUMER_ACCEPT)?,
            on_unlinked: JavaCallback::for_method(&env, on_unlinked, ON_UNLINKED)?,
            take: JavaCallback::for_method(&env, take, BI_CONSUMER_ACCEPT)?,
            drop: JavaCallback::for_method(&env, drop, BI_CONSUMER_ACCEPT)?,
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
        dispatch: &GlobalRef,
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
        dispatch: &GlobalRef,
    ) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer_exact(key) }?;
        void_fn(env, &mut self.on_remove, &[buffer.into(), dispatch.into()])
    }

    fn on_clear(&mut self, env: &JNIEnv, dispatch: &GlobalRef) -> Result<(), Error> {
        void_fn(env, &mut self.on_clear, &[dispatch.into()])
    }

    fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_unlinked, &[])
    }

    fn take(&mut self, env: &JNIEnv, n: jint, dispatch: &GlobalRef) -> Result<(), Error> {
        self.take.execute(env, |init, obj| {
            let cnt = value_of_int(env, n)?;
            init.void().invoke(env, obj, &[cnt, dispatch.into()])
        })
    }

    fn drop(&mut self, env: &JNIEnv, n: jint, dispatch: &GlobalRef) -> Result<(), Error> {
        self.drop.execute(env, |init, obj| {
            let cnt = value_of_int(env, n)?;
            init.void().invoke(env, obj, &[cnt, dispatch.into()])
        })
    }
}

fn value_of_int<'l>(env: &'l JNIEnv, n: jint) -> Result<JValue<'l>, Error> {
    env.call_static_method(
        "java/lang/Integer",
        "valueOf",
        "(I)Ljava/lang/Integer;",
        &[n.into()],
    )
}
