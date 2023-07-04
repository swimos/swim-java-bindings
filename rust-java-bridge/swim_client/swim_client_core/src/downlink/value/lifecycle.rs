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

use bytes::BytesMut;
use jni::errors::Error;
use jni::sys::jobject;
use jni::JNIEnv;
use jvm_sys::EnvExt;

use crate::downlink::vtable::{void_fn, JavaMethod, CONSUMER_ACCEPT, ON_LINKED, ON_UNLINKED};
use jvm_sys::vm::{jni_call, SpannedError};

pub struct ValueDownlinkLifecycle {
    vtable: ValueDownlinkVTable,
}

impl ValueDownlinkLifecycle {
    pub fn from_parts(
        env: &JNIEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> Result<ValueDownlinkLifecycle, Error> {
        Ok(ValueDownlinkLifecycle {
            vtable: ValueDownlinkVTable::new(
                env,
                on_event,
                on_linked,
                on_set,
                on_synced,
                on_unlinked,
            )?,
        })
    }

    pub fn on_linked(&mut self, env: &JNIEnv) -> Result<(), SpannedError> {
        let ValueDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.on_linked(env))
    }

    pub fn on_synced(
        &mut self,
        env: &JNIEnv,
        value: &mut Vec<u8>,
        _ret: &mut BytesMut,
    ) -> Result<(), SpannedError> {
        let ValueDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.on_synced(env, value))
    }

    pub fn on_event(
        &mut self,
        env: &JNIEnv,
        value: &mut Vec<u8>,
        _ret: &mut BytesMut,
    ) -> Result<(), SpannedError> {
        let ValueDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.on_event(env, value))
    }

    pub fn on_set(
        &mut self,
        env: &JNIEnv,
        value: &mut Vec<u8>,
        _ret: &mut BytesMut,
    ) -> Result<(), SpannedError> {
        let ValueDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.on_set(env, value))
    }

    pub fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), SpannedError> {
        let ValueDownlinkLifecycle { vtable } = self;
        jni_call(env, || vtable.on_unlinked(env))
    }
}

pub struct ValueDownlinkVTable {
    on_linked: JavaMethod,
    on_synced: JavaMethod,
    on_event: JavaMethod,
    on_set: JavaMethod,
    on_unlinked: JavaMethod,
}

impl ValueDownlinkVTable {
    fn new(
        env: &JNIEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> Result<ValueDownlinkVTable, Error> {
        Ok(ValueDownlinkVTable {
            on_linked: JavaMethod::for_method(&env, on_linked, ON_LINKED)?,
            on_synced: JavaMethod::for_method(&env, on_synced, CONSUMER_ACCEPT)?,
            on_event: JavaMethod::for_method(&env, on_event, CONSUMER_ACCEPT)?,
            on_set: JavaMethod::for_method(&env, on_set, CONSUMER_ACCEPT)?,
            on_unlinked: JavaMethod::for_method(&env, on_unlinked, ON_UNLINKED)?,
        })
    }

    fn on_linked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_linked, &[])
    }

    fn on_synced(&mut self, env: &JNIEnv, value: &mut Vec<u8>) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer_exact(value) }?;
        void_fn(env, &mut self.on_synced, &[buffer.into()])
    }

    fn on_event(&mut self, env: &JNIEnv, value: &mut Vec<u8>) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer_exact(value) }?;
        void_fn(env, &mut self.on_event, &[buffer.into()])
    }

    fn on_set(&mut self, env: &JNIEnv, value: &mut Vec<u8>) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer_exact(value) }?;
        void_fn(env, &mut self.on_set, &[buffer.into()])
    }

    fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_unlinked, &[])
    }
}
