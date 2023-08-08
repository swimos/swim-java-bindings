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

use std::panic::panic_any;
use std::process::abort;
use std::sync::Arc;

use jni::errors::{Error, JniError};
use jni::objects::{GlobalRef, JObject};
use jni::sys::jobject;
use jni::{JNIEnv, JavaVM};

use crate::vm::SpannedError;
use crate::JniResult;

pub trait VmExt {
    fn get_env(&self) -> JniResult<JNIEnv>;

    fn env_or_abort(&self) -> JNIEnv {
        match self.get_env() {
            Ok(env) => env,
            Err(_) => abort(),
        }
    }
}

pub trait UnwrapOrPanic<O> {
    fn unwrap_or_panic(self) -> O;
}

impl<O> UnwrapOrPanic<O> for Result<O, SpannedError> {
    fn unwrap_or_panic(self) -> O {
        match self {
            Ok(o) => o,
            Err(e) => panic_any(e),
        }
    }
}

impl VmExt for JavaVM {
    fn get_env(&self) -> JniResult<JNIEnv> {
        match JavaVM::get_env(self) {
            Ok(env) => Ok(env),
            Err(Error::JniCall(JniError::ThreadDetached)) => self.attach_current_thread_as_daemon(),
            Err(e) => Err(e),
        }
    }
}

impl<V> VmExt for Arc<V>
where
    V: VmExt,
{
    fn get_env(&self) -> JniResult<JNIEnv> {
        self.as_ref().get_env()
    }
}

#[allow(clippy::not_unsafe_ptr_arg_deref)]
pub fn new_global_ref(env: &JNIEnv, ptr: jobject) -> Result<Option<GlobalRef>, Error> {
    if ptr.is_null() {
        Ok(None)
    } else {
        unsafe { env.new_global_ref(JObject::from_raw(ptr)).map(Some) }
    }
}
