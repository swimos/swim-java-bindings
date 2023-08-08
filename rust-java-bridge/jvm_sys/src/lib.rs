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

extern crate core;

use jni::errors::Result as JniResult;
use jni::objects::{JByteBuffer, JObject, JValue};
use jni::JNIEnv;
use std::marker::PhantomData;

mod macros;
pub mod util;
pub mod vm;
pub use macros::*;
pub use paste;

pub trait EnvExt<'e> {
    unsafe fn new_direct_byte_buffer_exact<'b>(
        &'e self,
        buf: &'b mut Vec<u8>,
    ) -> JniResult<ByteBufferGuard<'b>>
    where
        'e: 'b;
}

impl<'e> EnvExt<'e> for JNIEnv<'e> {
    unsafe fn new_direct_byte_buffer_exact<'b>(
        &'e self,
        buf: &'b mut Vec<u8>,
    ) -> JniResult<ByteBufferGuard<'b>>
    where
        'e: 'b,
    {
        self.new_direct_byte_buffer(buf.as_mut_ptr(), buf.len())
            .map(move |buffer| ByteBufferGuard {
                _buf: Default::default(),
                buffer,
            })
    }
}

pub struct ByteBufferGuard<'b> {
    _buf: PhantomData<&'b mut Vec<u8>>,
    buffer: JByteBuffer<'b>,
}

impl<'b> From<ByteBufferGuard<'b>> for JValue<'b> {
    fn from(value: ByteBufferGuard<'b>) -> Self {
        unsafe { JValue::Object(JObject::from_raw(value.buffer.into_raw())) }
    }
}
