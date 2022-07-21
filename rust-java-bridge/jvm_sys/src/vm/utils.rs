// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use crate::JniResult;
use jni::errors::JniError;
use jni::{JNIEnv, JavaVM};

#[inline]
pub fn get_env(vm: &JavaVM) -> JniResult<JNIEnv> {
    match vm.get_env() {
        Ok(env) => Ok(env),
        Err(jni::errors::Error::JniCall(JniError::ThreadDetached)) => {
            // println!("Daemon");
            vm.attach_current_thread_as_daemon()
        }
        Err(e) => Err(e),
    }
}
