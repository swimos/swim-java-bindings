use jni::objects::JString;
use jni::sys::jbyteArray;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::null_pointer_check_abort;
use swim_server_core::agent::context::JavaAgentContext;
use swim_server_core::agent::spec::LaneSpec;
use swim_server_core::server_fn;

server_fn! {
    agent_AgentContextFunctionTable_openLane(
        env,
        _class,
        context: *mut JavaAgentContext,
        lane_uri: JString,
        config: jbyteArray
    ) {
        null_pointer_check_abort!(env, context, lane_uri, config);

        let context = unsafe { &*context };
        let env = context.env();
        let spec = match LaneSpec::try_from_jbyte_array::<()>(&env, config) {
            Ok(spec) => spec,
            Err(_) => return,
        };

        env.with_env(|scope| {
            let lane_uri_str = scope.get_rust_string(lane_uri);
            context.open_lane(lane_uri_str, spec);
        });
    }
}

server_fn! {
    agent_AgentContextFunctionTable_dropHandle(
        env,
        _class,
        context: *mut JavaAgentContext,
    ) {
        null_pointer_check_abort!(env, context);
        unsafe {
            drop(Box::from_raw(context));
        }
    }
}
