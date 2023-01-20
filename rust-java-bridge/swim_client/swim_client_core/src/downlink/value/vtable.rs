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
use jni::JNIEnv;

use jvm_sys::vm::method::{
    InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};
use jvm_sys::vm::utils::new_global_ref;
use jvm_sys::vm::with_local_frame_null;

const ON_LINKED: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnLinked", "onLinked", "()V");
const ON_UNLINKED: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnUnlinked", "onUnlinked", "()V");
const FUNCTION_APPLY: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "java/util/function/Function",
    "apply",
    "(Ljava/lang/Object;)Ljava/lang/Object;",
);

struct JavaFunction {
    ptr: Option<GlobalRef>,
    def: FunctionDefinition,
}

enum FunctionDefinition {
    Unit(JavaObjectMethodDef),
    Init(InitialisedJavaObjectMethod),
}

impl JavaFunction {
    pub fn for_method(
        env: &JNIEnv,
        ptr: jobject,
        method: JavaObjectMethodDef,
    ) -> Result<JavaFunction, Error> {
        Ok(JavaFunction {
            ptr: new_global_ref(&env, ptr)?,
            def: FunctionDefinition::Unit(method),
        })
    }

    pub fn execute<'j, F>(&mut self, env: &'j JNIEnv, f: F) -> Result<(), Error>
    where
        F: FnOnce(&mut InitialisedJavaObjectMethod, JObject) -> Result<(), Error>,
    {
        let JavaFunction { ptr, def } = self;
        match ptr {
            Some(ptr) => match def {
                FunctionDefinition::Unit(inner) => {
                    let mut initialised = inner.initialise(env)?;
                    let result =
                        with_local_frame_null(env, None, || f(&mut initialised, ptr.as_obj()));
                    *def = FunctionDefinition::Init(initialised);
                    result
                }
                FunctionDefinition::Init(inner) => {
                    with_local_frame_null(env, None, || f(inner, ptr.as_obj()))
                }
            },
            None => Ok(()),
        }
    }
}

// notes:
//  - there are three options for methods that accept byte buffers:
//       - the java side provides a shared channel that both rust and java read and write to.
//           to maximise efficiency here, the bridge between the lane/downlink rx channel and
//           the ffi receiver could wait before making the call to ensure that the buffer is
//           full or the end of a message has been received.
//       - rust creates a byte buffer that is backed by the contents that have been read out
//           of the lane's/downlink's buffer and then convert it to direct byte buffer for
//           the ffi call, which java then uses to return any data.
//       - a two-step approach where rust asks java to allocate an array with a capacity of
//           the message length, which it then populates and calls through with.
//
//       The most efficient way at the moment seems to be just wrapping the received bytes in
//       a byte buffer as it's zero-copy and both rust and java have the whole message
//       available. This approach also allows for only a single FFI call to be made from rust
//       to java (excluding the allocation of the buffer object) as opposed to the multiple
//       calls that would be required to send the message over a channel.
//
//  - While the 'JavaFunction' implementation does lazily initialise itself (retrieving the
//    corresponding MethodID and parsing the function signature), it is preferable for this to be
//    done in some kind of proxy and for it to be initialised *once* and then all references to it
//    can use the initialised version; but this may require some kind of RwLock or an UnsafeCell or
//    for the proxy/vtable to initialise it on startup and then for referents to acquire an
//    initialised reference. This is something that should be implemented once the FFI module is
//    more flushed out. todo: proxy
pub struct ValueDownlinkVTable {
    on_linked: JavaFunction,
    on_synced: JavaFunction,
    on_event: JavaFunction,
    on_set: JavaFunction,
    on_unlinked: JavaFunction,
}

impl ValueDownlinkVTable {
    pub fn new(
        env: &JNIEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> Result<ValueDownlinkVTable, Error> {
        Ok(ValueDownlinkVTable {
            on_linked: JavaFunction::for_method(&env, on_linked, ON_LINKED)?,
            on_synced: JavaFunction::for_method(&env, on_synced, FUNCTION_APPLY)?,
            on_event: JavaFunction::for_method(&env, on_event, FUNCTION_APPLY)?,
            on_set: JavaFunction::for_method(&env, on_set, FUNCTION_APPLY)?,
            on_unlinked: JavaFunction::for_method(&env, on_unlinked, ON_UNLINKED)?,
        })
    }

    pub fn on_linked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_linked, &[])
    }

    pub fn on_synced(&mut self, env: &JNIEnv, value: &mut Vec<u8>) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer(value.as_mut_ptr(), value.len()) }?;
        null_fn(env, &mut self.on_synced, &[buffer.into()])
    }

    pub fn on_event(&mut self, env: &JNIEnv, value: &mut Vec<u8>) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer(value.as_mut_ptr(), value.len()) }?;
        null_fn(env, &mut self.on_event, &[buffer.into()])
    }

    pub fn on_set(&mut self, env: &JNIEnv, value: &mut Vec<u8>) -> Result<(), Error> {
        let buffer = unsafe { env.new_direct_byte_buffer(value.as_mut_ptr(), value.len()) }?;
        null_fn(env, &mut self.on_set, &[buffer.into()])
    }

    pub fn on_unlinked(&mut self, env: &JNIEnv) -> Result<(), Error> {
        void_fn(env, &mut self.on_unlinked, &[])
    }
}

fn void_fn(env: &JNIEnv, ptr: &mut JavaFunction, args: &[JValue<'_>]) -> Result<(), Error> {
    ptr.execute(env, |init, obj| init.void().invoke(env, obj, args))
}

fn null_fn(env: &JNIEnv, ptr: &mut JavaFunction, args: &[JValue<'_>]) -> Result<(), Error> {
    ptr.execute(env, |init, obj| init.null().invoke(env, obj, args))
}
