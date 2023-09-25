use crate::env::JavaEnv;
use crate::JniDefault;
use bytebridge::{ByteCodec, ByteCodecExt, FromBytesError};
use bytes::BytesMut;
use jni::sys::jbyteArray;

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
        let result = env.with_env_throw("ai/swim/server/codec/DecoderException", |scope| {
            let mut config_bytes = BytesMut::from_iter(scope.convert_byte_array(array));
            Ok::<B, FromBytesError>(B::try_from_bytes(&mut config_bytes)?)
        });

        match result {
            Ok(obj) => Ok(obj),
            Err(()) => Err(R::default()),
        }
    }
}
