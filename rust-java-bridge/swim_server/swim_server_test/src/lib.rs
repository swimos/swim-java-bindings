use jni::sys::jbyteArray;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::env::JavaEnv;
use jvm_sys::null_pointer_check_abort;
use swim_server_core::agent::spec::PlaneSpec;
use swim_server_core::server_fn;

mod agent;
mod mock;

server_fn! {
    SwimServerTest_forPlane(
        env,
        _class,
        config: jbyteArray
    ) {
        null_pointer_check_abort!(env, config);

        let env = JavaEnv::new(env);

        let _r = PlaneSpec::try_from_jbyte_array::<()>(&env, config);
    }
}
