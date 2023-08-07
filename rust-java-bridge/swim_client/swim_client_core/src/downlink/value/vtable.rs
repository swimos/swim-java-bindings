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

use jni::objects::{GlobalRef, JObject, JThrowable, JValue};
use jni::sys::jobject;
use jvm_sys::env::{IsTypeOfExceptionHandler, JavaEnv, JavaExceptionHandler, Scope};
use swim_api::error::DownlinkTaskError;

use jvm_sys::method::{
    InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};
use jvm_sys::vtable::Consumer;

struct JavaMethod {
    ptr: Option<GlobalRef>,
    def: InitialisedJavaObjectMethod,
}

impl JavaMethod {
    pub fn for_method(env: &JavaEnv, ptr: jobject, method: JavaObjectMethodDef) -> JavaMethod {
        let (ptr, def) = env.with_env(|scope| {
            let ptr = if ptr.is_null() {
                None
            } else {
                unsafe { Some(scope.new_global_ref(JObject::from_raw(ptr))) }
            };

            let method = scope.resolve(method);
            (ptr, method)
        });
        JavaMethod { ptr, def }
    }

    #[track_caller]
    pub fn execute<'j, H>(
        &mut self,
        handler: &H,
        scope: &Scope,
        args: &[JValue<'j>],
    ) -> Result<(), H::Err>
    where
        H: JavaExceptionHandler,
    {
        let JavaMethod { ptr, def } = self;
        match ptr {
            Some(ptr) => def.v().invoke(handler, scope, ptr.as_obj(), args),
            None => Ok(()),
        }
    }
}

struct ExceptionHandler(IsTypeOfExceptionHandler);

impl JavaExceptionHandler for ExceptionHandler {
    type Err = DownlinkTaskError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        self.0
            .inspect(scope, throwable)
            .map(|e| DownlinkTaskError::Custom(Box::new(e)))
    }
}

pub struct ValueDownlinkVTable {
    on_linked: JavaMethod,
    on_synced: JavaMethod,
    on_event: JavaMethod,
    on_set: JavaMethod,
    on_unlinked: JavaMethod,
    handler: ExceptionHandler,
}

impl ValueDownlinkVTable {
    const ON_LINKED: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnLinked", "onLinked", "()V");
    const ON_UNLINKED: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnUnlinked", "onUnlinked", "()V");

    pub fn new(
        env: &JavaEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> ValueDownlinkVTable {
        ValueDownlinkVTable {
            on_linked: JavaMethod::for_method(env, on_linked, Self::ON_LINKED),
            on_synced: JavaMethod::for_method(env, on_synced, Consumer::ACCEPT),
            on_event: JavaMethod::for_method(env, on_event, Consumer::ACCEPT),
            on_set: JavaMethod::for_method(env, on_set, Consumer::ACCEPT),
            on_unlinked: JavaMethod::for_method(env, on_unlinked, Self::ON_UNLINKED),
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
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(value) };
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
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(value) };
            on_event.execute(handler, &scope, &[buffer.into()])
        })
    }

    pub fn on_set(&mut self, env: &JavaEnv, value: &mut Vec<u8>) -> Result<(), DownlinkTaskError> {
        let ValueDownlinkVTable {
            on_set, handler, ..
        } = self;
        env.with_env(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(value) };
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
