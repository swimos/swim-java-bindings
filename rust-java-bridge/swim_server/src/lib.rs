use jni::objects::JString;
use jni::sys::jbyteArray;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::null_pointer_check_abort;
use swim_server_core::agent::context::JavaAgentContext;
use swim_server_core::agent::spec::LaneSpec;
use swim_server_core::server_fn;

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
    pub fn agent_AgentContextFunctionTable_openLane(
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
    /// Drops the [`JavaAgentContext`].
    ///
    /// # Arguments
    /// `context` - the Java agent's pointer to the context.
    pub fn agent_AgentContextFunctionTable_dropHandle(
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

server_fn! {
    /// Registers a new task be run after an interval.
    ///
    /// # Arguments
    /// `context` - the Java agent's pointer to the context.
    /// `resume_after_seconds` - interval seconds resolution.
    /// `resume_after_nanos` - interval nanoseconds resolution.
    /// `id_msb` - the most significant bits in the task's UUID.
    /// `id_lsb` - the least significant bits in the task's UUID.
    pub fn agent_AgentContextFunctionTable_suspendTask(
        env,
        _class,
        context: *mut JavaAgentContext,
        resume_after_seconds: u64,
        resume_after_nanos: u32,
        id_msb: u64,
        id_lsb: u64,
    ) {
        null_pointer_check_abort!(env, context);

        let context = unsafe { &*context };
        context.suspend_task(resume_after_seconds, resume_after_nanos, id_msb, id_lsb);
    }
}

server_fn! {
    /// Registers a new task be run indefinitely with an interval between invocations.
    ///
    /// # Arguments
    /// `context` - the Java agent's pointer to the context.
    /// `resume_after_seconds` - interval seconds resolution.
    /// `resume_after_nanos` - interval nanoseconds resolution.
    /// `id_msb` - the most significant bits in the task's UUID.
    /// `id_lsb` - the least significant bits in the task's UUID.
    pub fn agent_AgentContextFunctionTable_scheduleTaskIndefinitely(
        env,
        _class,
        context: *mut JavaAgentContext,
        interval_seconds: u64,
        interval_nanos: u32,
        id_msb: u64,
        id_lsb: u64,
    ) {
        null_pointer_check_abort!(env, context);

        let context = unsafe { &*context };
        context.schedule_task_indefinitely(interval_seconds, interval_nanos, id_msb, id_lsb);
    }
}

server_fn! {
    /// Registers a new task be run for a fixed number of times with an interval between invocations.
    ///
    /// # Arguments
    /// `context` - the Java agent's pointer to the context.
    /// `count` - the number of times to run the task.
    /// `resume_after_seconds` - interval seconds resolution.
    /// `resume_after_nanos` - interval nanoseconds resolution.
    /// `id_msb` - the most significant bits in the task's UUID.
    /// `id_lsb` - the least significant bits in the task's UUID.
    pub fn agent_AgentContextFunctionTable_repeatTask(
        env,
        _class,
        context: *mut JavaAgentContext,
        count: usize,
        interval_seconds: u64,
        interval_nanos: u32,
        id_msb: u64,
        id_lsb: u64,
    ) {
        null_pointer_check_abort!(env, context);

        let context = unsafe { &*context };
        context.repeat_task(count, interval_seconds, interval_nanos, id_msb, id_lsb);
    }
}

server_fn! {
    /// Cancels the task associated with the provided UUID.
    ///
    /// # Arguments
    /// `context` - the Java agent's pointer to the context.
    /// `id_msb` - the most significant bits in the task's UUID.
    /// `id_lsb` - the least significant bits in the task's UUID.
    pub fn agent_AgentContextFunctionTable_cancelTask(
        env,
        _class,
        context: *mut JavaAgentContext,
        id_msb: u64,
        id_lsb: u64,
    ) {
        null_pointer_check_abort!(env, context);

        let context = unsafe { &*context };
        context.cancel_task(id_msb, id_lsb);
    }
}
