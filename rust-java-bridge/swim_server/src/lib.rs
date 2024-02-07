use jni::objects::JString;
use jni::sys::jbyteArray;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::null_pointer_check_abort;
use swim_server_core::server_fn;
use swim_server_core::spec::LaneSpec;
use swim_server_core::JavaAgentContext;

server_fn! {
    /// Opens a new lane on the agent.
    ///
    /// # Arguments
    /// - 'context' - agent-scoped context.
    /// - 'lane_uri' - the URI of the lane. This must not already exist in the agent or an exception
    /// will be thrown.
    /// - 'config' - msgpack representation of the lane.
    ///
    /// # Throws:
    /// - "ai/swim/server/codec/DecoderException" if 'config' is malformed. This will be propagated back
    /// to the agent runtime and cause the server runtime to shutdown as a malformed buffer can only
    /// occur as the result of a bug.
    ///
    /// # Blocking
    /// Blocks the current thread until there is sufficient capacity in the channel to the agent runtime
    /// for the request.
    fn agent_AgentContextFunctionTable_openLane(
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
    /// Drops the JavaAgentContext.
    ///
    /// Care must be taken to ensure that this function is only called once per JavaAgentContext
    /// instance.
    fn agent_AgentContextFunctionTable_dropHandle(
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
