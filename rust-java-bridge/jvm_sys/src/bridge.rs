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

use crate::env::JavaEnv;
use crate::JniDefault;
use bytebridge::{ByteCodec, ByteCodecExt, FromBytesError};
use bytes::BytesMut;
use jni::sys::jbyteArray;

const DECODER_EXCEPTION: &str = "ai/swim/server/codec/DecoderException";

pub trait JniByteCodec: ByteCodec {
    fn try_from_jbyte_array<R>(env: &JavaEnv, array: jbyteArray) -> Result<Self, R>
    where
        Self: Sized,
        R: JniDefault;
}

impl<B> JniByteCodec for B
where
    B: ByteCodec,
{
    fn try_from_jbyte_array<R>(env: &JavaEnv, array: jbyteArray) -> Result<Self, R>
    where
        Self: Sized,
        R: JniDefault,
    {
        let result = env.with_env_throw(DECODER_EXCEPTION, |scope| {
            let mut config_bytes = BytesMut::from_iter(scope.convert_byte_array(array));
            B::try_from_bytes(&mut config_bytes)
        });

        match result {
            Ok(obj) => Ok(obj),
            Err(()) => Err(R::default()),
        }
    }
}
