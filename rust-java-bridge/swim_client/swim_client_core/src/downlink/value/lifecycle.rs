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
use jni::sys::jobject;
use jvm_sys::env::JavaEnv;

use crate::downlink::value::vtable::ValueDownlinkVTable;

pub struct ValueDownlinkLifecycle {
    vtable: ValueDownlinkVTable,
}

impl ValueDownlinkLifecycle {
    pub fn from_parts(
        env: &JavaEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> ValueDownlinkLifecycle {
        ValueDownlinkLifecycle {
            vtable: ValueDownlinkVTable::new(
                env,
                on_event,
                on_linked,
                on_set,
                on_synced,
                on_unlinked,
            ),
        }
    }

    pub fn on_linked(&mut self, env: &JavaEnv) {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_linked(env)
    }

    pub fn on_synced(&mut self, env: &JavaEnv, value: &mut Vec<u8>, _ret: &mut BytesMut) {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_synced(env, value)
    }

    pub fn on_event(&mut self, env: &JavaEnv, value: &mut Vec<u8>, _ret: &mut BytesMut) {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_event(env, value)
    }

    pub fn on_set(&mut self, env: &JavaEnv, value: &mut Vec<u8>, _ret: &mut BytesMut) {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_set(env, value)
    }

    pub fn on_unlinked(&mut self, env: &JavaEnv) {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_unlinked(env)
    }
}
