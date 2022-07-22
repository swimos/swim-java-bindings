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
use jni::strings::JNIString;
use jni::JNIEnv;

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

    fn throw_new<S>(&self, env: &JNIEnv) -> JniResult<()>
    where
        S: Into<JNIString>;
}

impl ThrowException for JavaException {
    fn throw(&self, env: &JNIEnv) -> JniResult<()> {
        env.throw(self.class)
    }

    fn throw_new<S>(&self, env: &JNIEnv) -> JniResult<()>
    where
        S: Into<JNIString>,
    {
        let JavaException {
            class,
            default_message,
        } = self;
        env.throw_new(class, default_message)
    }
}
