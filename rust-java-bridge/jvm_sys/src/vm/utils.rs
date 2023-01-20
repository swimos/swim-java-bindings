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

use std::sync::Arc;

use jni::errors::{Error, JniError};
use jni::objects::{GlobalRef, JObject};
use jni::sys::jobject;
use jni::{JNIEnv, JavaVM};

use crate::JniResult;

#[inline]
pub fn get_env(vm: &JavaVM) -> JniResult<JNIEnv> {
    match vm.get_env() {
        Ok(env) => Ok(env),
        Err(Error::JniCall(JniError::ThreadDetached)) => vm.attach_current_thread_as_daemon(),
        Err(e) => Err(e),
    }
}

#[inline]
pub fn get_env_shared(vm: &Arc<JavaVM>) -> JniResult<JNIEnv> {
    get_env(vm.as_ref())
}

#[inline]
pub fn get_env_shared_expect(vm: &Arc<JavaVM>) -> JNIEnv {
    get_env_shared(vm).expect("Failed to get JNI environment interface")
}

pub fn new_global_ref(env: &JNIEnv, ptr: jobject) -> Result<Option<GlobalRef>, Error> {
    if ptr.is_null() {
        Ok(None)
    } else {
        unsafe { env.new_global_ref(JObject::from_raw(ptr)).map(Some) }
    }
}
