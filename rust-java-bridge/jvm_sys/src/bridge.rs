use crate::{jni_try, JniDefault};
use bytebridge::ByteCodec;
use bytes::BytesMut;
use jni::sys::jbyteArray;
use jni::JNIEnv;

pub trait JniByteCodec: ByteCodec {
    fn try_from_jbyte_array<R>(env: &JNIEnv, array: jbyteArray) -> Result<Self, R>
    where
        Self: Sized,
        R: JniDefault;
}

impl<B> JniByteCodec for B
where
    B: ByteCodec,
{
    fn try_from_jbyte_array<R>(env: &JNIEnv, array: jbyteArray) -> Result<Self, R>
    where
        Self: Sized,
        R: JniDefault,
    {
        let mut config_bytes = jni_try! {
            env,
            "Failed to parse configuration array",
            env.convert_byte_array(array).map(BytesMut::from_iter),
            Err(R::default())
        };

        match B::try_from_bytes(&mut config_bytes) {
            Ok(ty) => Ok(ty),
            Err(e) => {
                env.throw(format!("{:?}", e))
                    .expect("Failed to throw exception");
                Err(R::default())
            }
        }
    }
}
