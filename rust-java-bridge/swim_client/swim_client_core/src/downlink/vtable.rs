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

pub const ON_LINKED: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnLinked", "onLinked", "()V");
pub const ON_UNLINKED: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnUnlinked", "onUnlinked", "()V");
pub const CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "java/util/function/Consumer",
    "accept",
    "(Ljava/lang/Object;)V",
);
pub const BI_CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "java/util/function/BiConsumer",
    "accept",
    "(Ljava/lang/Object;Ljava/lang/Object;)V",
);
pub const TRI_CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "ai/swim/client/downlink/TriConsumer",
    "accept",
    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
);
pub const ROUTINE_EXEC: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/downlink/map/Routine", "exec", "()V");

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
pub struct JavaMethod {
    ptr: Option<GlobalRef>,
    def: MethodDefinition,
}

enum MethodDefinition {
    Unit(JavaObjectMethodDef),
    Init(InitialisedJavaObjectMethod),
}

impl JavaMethod {
    pub fn for_method(
        env: &JNIEnv,
        ptr: jobject,
        method: JavaObjectMethodDef,
    ) -> Result<JavaMethod, Error> {
        Ok(JavaMethod {
            ptr: new_global_ref(&env, ptr)?,
            def: MethodDefinition::Unit(method),
        })
    }

    pub fn execute<'j, F>(&mut self, env: &'j JNIEnv, f: F) -> Result<(), Error>
    where
        F: FnOnce(&mut InitialisedJavaObjectMethod, JObject) -> Result<(), Error>,
    {
        let JavaMethod { ptr, def } = self;
        match ptr {
            Some(ptr) => match def {
                MethodDefinition::Unit(inner) => {
                    let mut initialised = inner.initialise(env)?;
                    let result =
                        with_local_frame_null(env, None, || f(&mut initialised, ptr.as_obj()));
                    *def = MethodDefinition::Init(initialised);
                    result
                }
                MethodDefinition::Init(inner) => {
                    with_local_frame_null(env, None, || f(inner, ptr.as_obj()))
                }
            },
            None => Ok(()),
        }
    }
}

pub fn void_fn(env: &JNIEnv, ptr: &mut JavaMethod, args: &[JValue<'_>]) -> Result<(), Error> {
    ptr.execute(env, |init, obj| init.void().invoke(env, obj, args))
}
