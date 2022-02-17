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

use std::sync::atomic::AtomicBool;
use std::sync::Arc;

use crate::sync::ordering;
use jni::objects::JClass;
use jni::sys::{jboolean, jint};
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicBool_createNative(
    _env: JNIEnv,
    _class: JClass,
    initial: jboolean,
) -> *mut Arc<AtomicBool> {
    let initial = if initial == 0 {
        false
    } else if initial == 1 {
        true
    } else {
        unreachable!()
    };

    Box::into_raw(Box::new(Arc::new(AtomicBool::new(initial))))
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicBool_destroyNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Arc<AtomicBool>,
) {
    unsafe {
        Box::from_raw(ptr);
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicBool_loadNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Arc<AtomicBool>,
    i: jint,
) -> jboolean {
    unsafe { (&*ptr).load(ordering(i)).into() }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicBool_storeNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Arc<AtomicBool>,
    val: jboolean,
    i: jint,
) {
    let value = if val == 0 {
        false
    } else if val == 1 {
        true
    } else {
        unreachable!()
    };
    unsafe { (&*ptr).store(value, ordering(i)) }
}
