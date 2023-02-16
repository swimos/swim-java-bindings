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
use jni::objects::JByteBuffer;
use jni::JNIEnv;

// pub mod bytes;
mod macros;
pub mod util;
pub mod vm;
pub use macros::*;
pub use paste;

pub trait EnvExt<'a> {
    unsafe fn new_direct_byte_buffer_exact(
        &'a self,
        buf: &mut Vec<u8>,
    ) -> JniResult<JByteBuffer<'a>>;
}

impl<'a> EnvExt<'a> for JNIEnv<'a> {
    unsafe fn new_direct_byte_buffer_exact(
        &'a self,
        buf: &mut Vec<u8>,
    ) -> JniResult<JByteBuffer<'a>> {
        self.new_direct_byte_buffer(buf.as_mut_ptr(), buf.len())
    }
}
