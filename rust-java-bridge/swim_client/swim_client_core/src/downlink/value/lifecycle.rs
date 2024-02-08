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

use bytes::BytesMut;
use jni::sys::jobject;
use jvm_sys::env::{IsTypeOfExceptionHandler, JavaEnv};
use swim_api::error::DownlinkTaskError;

use crate::downlink::{ON_LINKED, ON_UNLINKED};
use jvm_sys::vtable::Consumer;

use crate::downlink::vtable::{ExceptionHandler, JavaMethod};

pub struct ValueDownlinkVTable {
    on_linked: JavaMethod,
    on_synced: JavaMethod,
    on_event: JavaMethod,
    on_set: JavaMethod,
    on_unlinked: JavaMethod,
    handler: ExceptionHandler,
}

impl ValueDownlinkVTable {
    pub fn new(
        env: &JavaEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> ValueDownlinkVTable {
        ValueDownlinkVTable {
            on_linked: JavaMethod::for_method(env, on_linked, ON_LINKED),
            on_synced: JavaMethod::for_method(env, on_synced, Consumer::ACCEPT),
            on_event: JavaMethod::for_method(env, on_event, Consumer::ACCEPT),
            on_set: JavaMethod::for_method(env, on_set, Consumer::ACCEPT),
            on_unlinked: JavaMethod::for_method(env, on_unlinked, ON_UNLINKED),
            handler: ExceptionHandler(IsTypeOfExceptionHandler::new(
                env,
                "ai/swim/client/downlink/DownlinkException",
            )),
        }
    }

    pub fn on_linked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkVTable {
            on_linked, handler, ..
        } = self;
        env.with_env(|scope| on_linked.execute(handler, &scope, &[]))
    }

    pub fn on_synced(
        &mut self,
        env: &JavaEnv,
        value: &mut Vec<u8>,
    ) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkVTable {
            on_synced, handler, ..
        } = self;
        env.with_env(|scope| {
            let buffer = scope.new_direct_byte_buffer_exact(value);
            on_synced.execute(handler, &scope, &[buffer.into()])
        })
    }

    pub fn on_event(
        &mut self,
        env: &JavaEnv,
        value: &mut Vec<u8>,
    ) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkVTable {
            on_event, handler, ..
        } = self;
        env.with_env(|scope| {
            let buffer = scope.new_direct_byte_buffer_exact(value);
            on_event.execute(handler, &scope, &[buffer.into()])
        })
    }

    pub fn on_set(&mut self, env: &JavaEnv, value: &mut Vec<u8>) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkVTable {
            on_set, handler, ..
        } = self;
        env.with_env(|scope| {
            let buffer = scope.new_direct_byte_buffer_exact(value);
            on_set.execute(handler, &scope, &[buffer.into()])
        })
    }

    pub fn on_unlinked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkVTable {
            on_unlinked,
            handler,
            ..
        } = self;
        env.with_env(|scope| on_unlinked.execute(handler, &scope, &[]))
    }
}

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

    pub fn on_linked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_linked(env)
    }

    pub fn on_synced(
        &mut self,
        env: &JavaEnv,
        value: &mut Vec<u8>,
        _ret: &mut BytesMut,
    ) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_synced(env, value)
    }

    pub fn on_event(
        &mut self,
        env: &JavaEnv,
        value: &mut Vec<u8>,
        _ret: &mut BytesMut,
    ) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_event(env, value)
    }

    pub fn on_set(
        &mut self,
        env: &JavaEnv,
        value: &mut Vec<u8>,
        _ret: &mut BytesMut,
    ) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_set(env, value)
    }

    pub fn on_unlinked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkLifecycle { vtable } = self;
        vtable.on_unlinked(env)
    }
}
