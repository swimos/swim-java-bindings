// Copyright 2015-2021 Swim Inc.
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

use jni::errors::Error as JniError;
use jni::errors::Result as JniResult;
use jni::objects::{JObject, JValue};
use jni::JNIEnv;

pub const METHOD_OBJECT_WAIT: JavaMethod = JavaMethod::new("wait", "()V");
pub const METHOD_OBJECT_NOTIFY: JavaMethod = JavaMethod::new("notify", "()V");

pub struct JavaMethod {
    name: &'static str,
    signature: &'static str,
}

impl JavaMethod {
    pub const fn new(name: &'static str, signature: &'static str) -> JavaMethod {
        JavaMethod { name, signature }
    }
}

pub trait InvokeObjectMethod {
    fn invoke<'o, O>(
        &self,
        env: &JNIEnv<'o>,
        object: O,
        args: &[JValue<'o>],
    ) -> Result<JValue<'o>, JniError>
    where
        O: Into<JObject<'o>>;
}

impl InvokeObjectMethod for JavaMethod {
    fn invoke<'o, O>(
        &self,
        env: &JNIEnv<'o>,
        object: O,
        args: &[JValue<'o>],
    ) -> Result<JValue<'o>, JniError>
    where
        O: Into<JObject<'o>>,
    {
        let JavaMethod { name, signature } = self;
        env.call_method(object, name, signature, args)
    }
}

pub struct JavaException {
    class: &'static str,
    default_message: &'static str,
}

impl JavaException {
    pub const fn new(class: &'static str, default_message: &'static str) -> JavaException {
        JavaException {
            class,
            default_message,
        }
    }
}

pub trait ThrowException {
    fn throw(&self, env: &JNIEnv) -> JniResult<()>;
}

impl ThrowException for JavaException {
    fn throw(&self, env: &JNIEnv) -> JniResult<()> {
        let JavaException {
            class,
            default_message,
        } = self;
        env.throw_new(class, default_message)
    }
}
