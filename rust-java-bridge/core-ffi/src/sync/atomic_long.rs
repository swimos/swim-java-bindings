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

use crate::sync::ordering;
use jni::objects::JClass;
use jni::sys::{jint, jlong};
use jni::JNIEnv;
use std::sync::atomic::AtomicU64;
use std::sync::Arc;

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicU64_createNative(
    _env: JNIEnv,
    _class: JClass,
    initial: jlong,
) -> *mut Arc<AtomicU64> {
    Box::into_raw(Box::new(Arc::new(AtomicU64::new(initial as u64))))
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicU64_destroyNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Arc<AtomicU64>,
) {
    unsafe {
        Box::from_raw(ptr);
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicU64_loadNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Arc<AtomicU64>,
    i: jint,
) -> jlong {
    (unsafe { (&*ptr).load(ordering(i)) as u64 }) as jlong
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_sync_RAtomicU64_storeNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Arc<AtomicU64>,
    value: jlong,
    i: jint,
) {
    unsafe { (&*ptr).store(value as u64, ordering(i)) }
}
