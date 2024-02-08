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

use jni::objects::{GlobalRef, JObject, JThrowable, JValue};
use jni::sys::jobject;
use jvm_sys::env::{IsTypeOfExceptionHandler, JavaEnv, JavaExceptionHandler, Scope};
use swim_api::error::DownlinkTaskError;

use jvm_sys::method::{
    InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};

pub struct JavaMethod {
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
    pub fn execute<H>(
        &mut self,
        handler: &H,
        scope: &Scope,
        args: &[JValue<'_>],
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

pub struct ExceptionHandler(pub IsTypeOfExceptionHandler);

impl JavaExceptionHandler for ExceptionHandler {
    type Err = DownlinkTaskError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        self.0
            .inspect(scope, throwable)
            .map(|e| DownlinkTaskError::Custom(Box::new(e)))
    }
}
