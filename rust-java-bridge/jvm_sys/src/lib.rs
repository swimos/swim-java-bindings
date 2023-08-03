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

use bytes::BytesMut;
use jni::errors::Result as JniResult;
use jni::objects::JByteBuffer;
use jni::JNIEnv;

pub mod bridge;
mod macros;
pub mod util;
pub mod vm;

pub use macros::*;
pub use paste;

pub trait BufPtr {
    fn as_mut_ptr(&mut self) -> *mut u8;

    fn len(&self) -> usize;
}

impl BufPtr for Vec<u8> {
    fn as_mut_ptr(&mut self) -> *mut u8 {
        Vec::as_mut_ptr(self)
    }

    fn len(&self) -> usize {
        Vec::len(self)
    }
}

impl BufPtr for BytesMut {
    fn as_mut_ptr(&mut self) -> *mut u8 {
        self.as_mut().as_mut_ptr()
    }

    fn len(&self) -> usize {
        BytesMut::len(self)
    }
}

pub trait EnvExt<'a> {
    unsafe fn new_direct_byte_buffer_exact<B>(&'a self, buf: &mut B) -> JniResult<JByteBuffer<'a>>
    where
        B: BufPtr;
}

impl<'a> EnvExt<'a> for JNIEnv<'a> {
    unsafe fn new_direct_byte_buffer_exact<B>(&'a self, buf: &mut B) -> JniResult<JByteBuffer<'a>>
    where
        B: BufPtr,
    {
        self.new_direct_byte_buffer(buf.as_mut_ptr(), buf.len())
    }
}
