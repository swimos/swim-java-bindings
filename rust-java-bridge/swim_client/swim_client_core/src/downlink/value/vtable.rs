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
use jni::objects::{GlobalRef, JObject, JValue};
use jni::sys::jobject;
use jvm_sys::env::{JavaEnv, Scope};

use jvm_sys::method::{
    InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};

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

    pub fn execute<'j>(&mut self, scope: &Scope, args: &[JValue<'j>]) -> Result<(), Error> {
        let JavaMethod { ptr, def } = self;
        match ptr {
            Some(ptr) => def.v().invoke(scope, ptr.as_obj(), args),
            None => Ok(()),
        }
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
    const ON_LINKED: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnLinked", "onLinked", "()V");
    const ON_UNLINKED: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnUnlinked", "onUnlinked", "()V");
    const CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "java/util/function/Consumer",
        "accept",
        "(Ljava/lang/Object;)V",
    );

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
            on_synced: JavaMethod::for_method(env, on_synced, Self::CONSUMER_ACCEPT),
            on_event: JavaMethod::for_method(env, on_event, Self::CONSUMER_ACCEPT),
            on_set: JavaMethod::for_method(env, on_set, Self::CONSUMER_ACCEPT),
            on_unlinked: JavaMethod::for_method(env, on_unlinked, Self::ON_UNLINKED),
        }
    }

    pub fn on_linked(&mut self, env: &JavaEnv) {
        env.with_env_expect(|scope| self.on_linked.execute(&scope, &[]))
    }

    pub fn on_synced(&mut self, env: &JavaEnv, value: &mut Vec<u8>) {
        env.with_env_expect(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(value) };
            self.on_synced.execute(&scope, &[buffer.into()])
        })
    }

    pub fn on_event(&mut self, env: &JavaEnv, value: &mut Vec<u8>) {
        env.with_env_expect(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(value) };
            self.on_event.execute(&scope, &[buffer.into()])
        })
    }

    pub fn on_set(&mut self, env: &JavaEnv, value: &mut Vec<u8>) {
        env.with_env_expect(|scope| {
            let buffer = unsafe { scope.new_direct_byte_buffer_exact(value) };
            self.on_set.execute(&scope, &[buffer.into()])
        })
    }

    pub fn on_unlinked(&mut self, env: &JavaEnv) {
        env.with_env_expect(|scope| self.on_unlinked.execute(&scope, &[]))
    }
}
