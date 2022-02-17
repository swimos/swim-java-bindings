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

use jni::objects::JClass;
use jni::JNIEnv;
use std::cell::RefCell;
use tokio::runtime::{Handle, Runtime};

thread_local! {
    static RUNTIME: RefCell<Option<Handle>> = RefCell::new(None)
}

#[no_mangle]
pub fn get_runtime() -> Result<Handle, ()> {
    match RUNTIME.try_with(|ctx| ctx.borrow().clone()) {
        Ok(Some(handle)) => Ok(handle),
        _ => panic!(),
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_RustRuntime_initRuntime(
    _env: JNIEnv,
    _class: JClass,
) -> *mut Runtime {
    println!("Building tokio runtime");

    let builder = tokio::runtime::Builder::new_multi_thread()
        .build()
        .expect("Failed to build Tokio runtime");
    let handle = builder.handle().clone();

    RUNTIME
        .try_with(|cell| cell.borrow_mut().replace(handle))
        .unwrap();

    Box::into_raw(Box::new(builder))
}
