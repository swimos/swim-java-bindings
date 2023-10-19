use std::future::Future;
use std::ops::Deref;
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use bytes::BytesMut;
use futures::ready;
use jni::objects::{GlobalRef, JObject, JThrowable, JValue};
use jni::sys::{jint, jobject};
use pin_project::pin_project;
use swim_api::error::{AgentInitError, AgentTaskError};
use swim_form::structural::read::ReadError;
use tokio::runtime::Handle;
use tokio::sync::mpsc;
use tokio::task::JoinHandle;
use tracing::{debug, trace};
use uuid::Uuid;

use jvm_sys::env::{
    IsTypeOfExceptionHandler, JavaEnv, JavaExceptionHandler, NotTypeOfExceptionHandler, Scope,
};
use jvm_sys::method::{
    ByteArray, InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};

use crate::agent::context::JavaAgentContext;
use crate::agent::foreign::buffer::Dispatcher;
use crate::agent::GuestRuntimeRequest;

pub trait AgentVTable: Send + Sync + 'static {
    type Suspended<O>: Future<Output = Result<O, AgentTaskError>> + Send
    where
        O: Send;

    fn did_start(&self) -> Self::Suspended<()>;

    fn did_stop(&self) -> Self::Suspended<()>;

    fn dispatch(&mut self, lane_id: i32, buffer: BytesMut) -> Self::Suspended<Vec<u8>>;

    fn sync(&self, lane_id: i32, remote: Uuid) -> Self::Suspended<Vec<u8>>;

    fn init(&mut self, lane_id: i32, msg: BytesMut) -> Self::Suspended<()>;

    fn flush_state(&self) -> Self::Suspended<Vec<u8>>;

    fn run_task(&self, id: Uuid) -> Self::Suspended<()>;
}

pub trait RuntimeContext: Send + Sync + Clone + 'static {
    fn fatal_error(&self, cause: impl ToString) -> AgentTaskError;
}

impl RuntimeContext for JavaEnv {
    fn fatal_error(&self, cause: impl ToString) -> AgentTaskError {
        self.fatal_error(cause)
    }
}

pub trait AgentFactory {
    type VTable: AgentVTable;
    type Context: RuntimeContext;

    fn agent_for(
        &self,
        ctx: Self::Context,
        uri: impl AsRef<str>,
        tx: mpsc::Sender<GuestRuntimeRequest>,
    ) -> Result<Self::VTable, AgentInitError>;
}

#[derive(Debug, Clone)]
pub struct JavaAgentFactory {
    new_agent_method: InitialisedJavaObjectMethod,
    factory: GlobalRef,
    vtable: Arc<JavaAgentVTable>,
}

impl JavaAgentFactory {
    const NEW_AGENT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/AbstractSwimServerBuilder",
        "agentFor",
        "(Ljava/lang/String;J)Lai/swim/server/agent/AgentView;",
    );

    pub fn new(env: &JavaEnv, factory: GlobalRef) -> JavaAgentFactory {
        JavaAgentFactory {
            new_agent_method: env.initialise(Self::NEW_AGENT),
            factory,
            vtable: Arc::new(JavaAgentVTable::initialise(env)),
        }
    }
}

impl AgentFactory for JavaAgentFactory {
    type VTable = JavaAgentRef;
    type Context = JavaEnv;

    fn agent_for(
        &self,
        env: Self::Context,
        uri: impl AsRef<str>,
        tx: mpsc::Sender<GuestRuntimeRequest>,
    ) -> Result<Self::VTable, AgentInitError> {
        let node_uri = uri.as_ref();
        debug!(node_uri, "Attempting to create a new Java agent");

        let JavaAgentFactory {
            new_agent_method,
            factory,
            vtable,
        } = self;

        let agent = env.with_env(|scope| {
            let java_agent_ctx = Box::leak(Box::new(JavaAgentContext::new(env.clone(), tx)));
            let java_uri = scope.new_string(node_uri);
            let obj_ref = unsafe {
                scope.invoke(
                    new_agent_method.l().global_ref(),
                    factory.as_obj(),
                    &[
                        JValue::Object(java_uri.deref().clone()),
                        JValue::Object(JObject::from_raw(
                            java_agent_ctx as *mut _ as u64 as jobject,
                        )),
                    ],
                )
            };
            JavaAgentRef::new(env.clone(), obj_ref, vtable.clone())
        });
        Ok(agent)
    }
}

#[pin_project]
struct AbortOnJoinError<O> {
    env: JavaEnv,
    #[pin]
    inner: JoinHandle<O>,
}

impl<O> AbortOnJoinError<O> {
    pub fn new(env: JavaEnv, inner: JoinHandle<O>) -> AbortOnJoinError<O> {
        AbortOnJoinError { env, inner }
    }
}

impl<O> Future for AbortOnJoinError<O> {
    type Output = O;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let projected = self.project();
        match ready!(projected.inner.poll(cx)) {
            Ok(o) => Poll::Ready(o),
            Err(e) => projected.env.fatal_error(e),
        }
    }
}

#[pin_project]
pub struct BlockingJniCall<O> {
    #[pin]
    inner: AbortOnJoinError<Result<O, AgentTaskError>>,
}

impl<O> BlockingJniCall<O> {
    pub fn new<F>(env: JavaEnv, func: F) -> BlockingJniCall<O>
    where
        F: FnOnce() -> Result<O, AgentTaskError> + Send + 'static,
        O: Send + 'static,
    {
        let handle = Handle::current();
        BlockingJniCall {
            inner: AbortOnJoinError::new(env, handle.spawn_blocking(func)),
        }
    }
}

impl<O> Future for BlockingJniCall<O> {
    type Output = Result<O, AgentTaskError>;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let projected = self.project();
        projected.inner.poll(cx)
    }
}

impl AgentVTable for JavaAgentRef {
    type Suspended<O> = BlockingJniCall<O>
    where
        O: Send;

    fn did_start(&self) -> Self::Suspended<()> {
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
            ..
        } = self;

        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();
        let env = env.clone();

        BlockingJniCall::new(env.clone(), move || {
            trace!("Invoking agent did_start method");
            env.with_env(|scope| vtable.did_start(&scope, agent_obj.as_obj()))
        })
    }

    fn did_stop(&self) -> Self::Suspended<()> {
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
            ..
        } = self;

        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();
        let env = env.clone();

        BlockingJniCall::new(env.clone(), move || {
            trace!("Invoking agent did_stop method");
            env.with_env(|scope| vtable.did_stop(&scope, agent_obj.as_obj()))
        })
    }

    fn dispatch(&mut self, lane_id: i32, msg: BytesMut) -> Self::Suspended<Vec<u8>> {
        let JavaAgentRef {
            dispatcher,
            env,
            agent_obj,
            vtable,
        } = self;

        let msg_len = msg.len();
        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();

        dispatcher.dispatch_to(env.clone(), msg, move |scope, buf_obj| {
            trace!("Dispatching event to agent");
            vtable.dispatch(&scope, agent_obj.as_obj(), lane_id, buf_obj, msg_len as i32)
        })
    }

    fn sync(&self, lane_id: i32, remote: Uuid) -> Self::Suspended<Vec<u8>> {
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
            ..
        } = self;

        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();
        let env = env.clone();

        BlockingJniCall::new(env.clone(), move || {
            trace!(lane_id, remote = %remote, "Dispatching sync for lane");
            env.with_env(|scope| vtable.sync(&scope, agent_obj.as_obj(), lane_id, remote))
        })
    }

    fn init(&mut self, lane_id: i32, msg: BytesMut) -> Self::Suspended<()> {
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
            dispatcher,
        } = self;

        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();
        let env = env.clone();

        dispatcher.dispatch_to(env.clone(), msg, move |scope, buf_obj| {
            trace!("Dispatching initialisation event to agent");

            vtable.init(&scope, agent_obj.as_obj(), lane_id, buf_obj);
            Ok(())
        })
    }

    fn flush_state(&self) -> Self::Suspended<Vec<u8>> {
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
            ..
        } = self;

        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();
        let env = env.clone();

        BlockingJniCall::new(env.clone(), move || {
            trace!("Flushing agent state");
            env.with_env(|scope| vtable.flush_state(&scope, agent_obj.as_obj()))
        })
    }

    fn run_task(&self, id: Uuid) -> Self::Suspended<()> {
        let JavaAgentRef {
            env,
            agent_obj,
            vtable,
            ..
        } = self;

        let vtable = vtable.clone();
        let agent_obj = agent_obj.clone();
        let env = env.clone();

        BlockingJniCall::new(env.clone(), move || {
            trace!("Flushing agent state");
            let (id_msb, id_lsb) = id.as_u64_pair();
            env.with_env(|scope| {
                vtable.run_task(&scope, agent_obj.as_obj(), id_msb as i64, id_lsb as i64)
            });
            Ok(())
        })
    }
}

mod buffer {
    use bytes::BytesMut;
    use jni::objects::JObject;
    use swim_api::error::AgentTaskError;
    use tokio::runtime::Handle;

    use jvm_sys::env::{GlobalRefByteBuffer, JObjectFromByteBuffer, JavaEnv, Scope};

    use crate::agent::foreign::{AbortOnJoinError, BlockingJniCall};

    #[derive(Debug)]
    pub struct Dispatcher {
        buffer_content: BytesMut,
        buffer_obj: GlobalRefByteBuffer,
    }

    impl Dispatcher {
        pub fn new(env: &JavaEnv, len: usize) -> Dispatcher {
            let mut buffer_content = BytesMut::with_capacity(len);
            unsafe {
                buffer_content.set_len(len);
            }
            let buffer_obj = env.with_env(|scope| unsafe {
                scope.new_direct_byte_buffer_global(
                    buffer_content.as_mut_ptr(),
                    buffer_content.len(),
                )
            });
            Dispatcher {
                buffer_content,
                buffer_obj,
            }
        }

        pub fn dispatch_to<F, O>(
            &mut self,
            env: JavaEnv,
            mut msg: BytesMut,
            method: F,
        ) -> BlockingJniCall<O>
        where
            F: FnOnce(Scope, JObject) -> Result<O, AgentTaskError> + Send + 'static,
            O: Send + 'static,
        {
            let Dispatcher {
                buffer_content,
                buffer_obj,
            } = self;
            let runtime_handle = Handle::current();

            let handle = if msg.len() > buffer_content.capacity() {
                let env = env.clone();
                runtime_handle.spawn_blocking(move || {
                    env.with_env(|scope| {
                        let temp_buffer = scope.new_direct_byte_buffer_exact(&mut msg);
                        let byte_buffer_ref = temp_buffer.as_byte_buffer();
                        let result = method(scope.clone(), byte_buffer_ref);

                        scope.delete_local_ref(byte_buffer_ref);

                        result
                    })
                })
            } else {
                buffer_content.clear();
                buffer_content.extend_from_slice(msg.as_ref());

                let byte_buffer = buffer_obj.clone();
                let env = env.clone();

                runtime_handle.spawn_blocking(move || {
                    env.with_env(|scope| method(scope, byte_buffer.as_byte_buffer()))
                })
            };
            BlockingJniCall {
                inner: AbortOnJoinError { env, inner: handle },
            }
        }
    }
}

#[derive(Debug)]
pub struct JavaAgentRef {
    dispatcher: Dispatcher,
    env: JavaEnv,
    agent_obj: GlobalRef,
    vtable: Arc<JavaAgentVTable>,
}

impl JavaAgentRef {
    pub fn new(env: JavaEnv, agent_obj: GlobalRef, vtable: Arc<JavaAgentVTable>) -> JavaAgentRef {
        // todo: size from config
        JavaAgentRef {
            dispatcher: Dispatcher::new(&env, 128),
            env,
            agent_obj,
            vtable,
        }
    }
}

#[derive(Debug)]
struct ExceptionHandler {
    user: NotTypeOfExceptionHandler,
    deserialization: IsTypeOfExceptionHandler,
}

impl JavaExceptionHandler for ExceptionHandler {
    type Err = AgentTaskError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        let ExceptionHandler {
            user,
            deserialization,
        } = self;

        debug!("Inspecting error");

        match deserialization.inspect(scope, throwable) {
            Some(err) => {
                debug!("Detected deserialization error");
                Some(AgentTaskError::DeserializationFailed(ReadError::Message(
                    err.to_string().into(),
                )))
            }
            None => user.inspect(scope, throwable).map(|err| {
                debug!("Detected user code error");
                AgentTaskError::UserCodeError(Box::new(err))
            }),
        }
    }
}

#[derive(Debug)]
pub struct JavaAgentVTable {
    did_start: InitialisedJavaObjectMethod,
    did_stop: InitialisedJavaObjectMethod,
    dispatch: InitialisedJavaObjectMethod,
    sync: InitialisedJavaObjectMethod,
    init: InitialisedJavaObjectMethod,
    run_task: InitialisedJavaObjectMethod,
    flush_state: InitialisedJavaObjectMethod,
    handler: ExceptionHandler,
}

impl JavaAgentVTable {
    const DID_START: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "didStart", "()V");
    const DID_STOP: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "didStop", "()V");
    const DISPATCH: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentView",
        "dispatch",
        "(ILjava/nio/ByteBuffer;I)[B",
    );
    const SYNC: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "sync", "(IJJ)[B");
    const INIT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "ai/swim/server/agent/AgentView",
        "init",
        "(ILjava/nio/ByteBuffer;)V",
    );
    const FLUSH_STATE: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "flushState", "()[B");
    const RUN_TASK: JavaObjectMethodDef =
        JavaObjectMethodDef::new("ai/swim/server/agent/AgentView", "runTask", "(IIZ)V");

    pub fn initialise(env: &JavaEnv) -> JavaAgentVTable {
        JavaAgentVTable {
            did_start: env.initialise(Self::DID_START),
            did_stop: env.initialise(Self::DID_STOP),
            dispatch: env.initialise(Self::DISPATCH),
            sync: env.initialise(Self::SYNC),
            init: env.initialise(Self::INIT),
            run_task: env.initialise(Self::RUN_TASK),
            flush_state: env.initialise(Self::FLUSH_STATE),
            handler: ExceptionHandler {
                user: NotTypeOfExceptionHandler::new(env, "ai/swim/server/agent/AgentException"),
                deserialization: IsTypeOfExceptionHandler::new(
                    env,
                    "ai/swim/server/agent/AgentException",
                ),
            },
        }
    }

    fn did_start(&self, scope: &Scope, agent_obj: JObject) -> Result<(), AgentTaskError> {
        let JavaAgentVTable {
            did_start, handler, ..
        } = self;
        did_start.v().invoke(handler, scope, agent_obj, &[])
    }

    fn did_stop(&self, scope: &Scope, agent_obj: JObject) -> Result<(), AgentTaskError> {
        let JavaAgentVTable {
            did_stop, handler, ..
        } = self;
        did_stop.v().invoke(handler, scope, agent_obj, &[])
    }

    fn dispatch(
        &self,
        scope: &Scope,
        agent_obj: JObject,
        lane_id: jint,
        msg: JObject,
        len: i32,
    ) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable {
            dispatch, handler, ..
        } = self;
        dispatch.l().array::<ByteArray>().invoke(
            handler,
            scope,
            agent_obj,
            &[lane_id.into(), msg.into(), len.into()],
        )
    }

    fn sync(
        &self,
        scope: &Scope,
        agent_obj: JObject,
        lane_id: jint,
        remote: Uuid,
    ) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable { sync, handler, .. } = self;
        let try_into = |num: u64| -> Result<i64, AgentTaskError> {
            match num.try_into() {
                Ok(n) => Ok(n),
                Err(_) => {
                    return Err(AgentTaskError::DeserializationFailed(
                        ReadError::NumberOutOfRange,
                    ))
                }
            }
        };

        let (msb, lsb) = remote.as_u64_pair();
        let msb = try_into(msb)?;
        let lsb = try_into(lsb)?;

        sync.l().array::<ByteArray>().invoke(
            handler,
            scope,
            agent_obj,
            &[lane_id.into(), msb.into(), lsb.into()],
        )
    }

    fn flush_state(&self, scope: &Scope, agent_obj: JObject) -> Result<Vec<u8>, AgentTaskError> {
        let JavaAgentVTable {
            flush_state,
            handler,
            ..
        } = self;
        flush_state
            .l()
            .array::<ByteArray>()
            .invoke(handler, scope, agent_obj, &[])
    }

    fn init(&self, scope: &Scope, agent_obj: JObject, lane_id: jint, msg: JObject) {
        let JavaAgentVTable { init, .. } = self;
        scope.invoke(init.v(), agent_obj, &[lane_id.into(), msg.into()])
    }

    fn run_task(&self, scope: &Scope, agent_obj: JObject, id_msb: i64, id_lsb: i64) {
        let JavaAgentVTable { run_task, .. } = self;
        scope.invoke(run_task.v(), agent_obj, &[id_msb.into(), id_lsb.into()])
    }
}
