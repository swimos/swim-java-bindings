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

use std::backtrace::Backtrace;
use std::collections::hash_map::Entry;
use std::collections::HashMap;
use std::convert::Infallible;
use std::error::Error as StdError;
use std::fmt;
use std::fmt::{Display, Formatter};
use std::marker::PhantomData;
use std::ops::Deref;
use std::panic::Location;
use std::process::abort;
use std::sync::Arc;

use bytes::BytesMut;
use jni::descriptors::Desc;
use jni::errors::{Error, JniError};
use jni::objects::{
    AutoLocal, GlobalRef, JByteBuffer, JClass, JMethodID, JObject, JString, JThrowable, JValue,
};
use jni::signature::ReturnType;
use jni::strings::{JNIString, JavaStr};
use jni::sys::{jbyteArray, jvalue};
use jni::{JNIEnv, JavaVM, MonitorGuard};
use parking_lot::Mutex;
use static_assertions::{assert_impl_all, assert_not_impl_all};
use tracing::{debug, error};

use crate::method::{
    InitialisedJavaObjectMethod, JavaMethodExt, JavaObjectMethod, JavaObjectMethodDef,
};
use crate::vtable::Throwable;

assert_impl_all!(JavaEnv: Send, Sync);

// JNIEnv's are scoped to a given thread and the Tokio runtime may yield an async task and return
// execution on another thread, so we uphold this in Scope and ensure that the current instance is
// not shared across threads; this is upheld directly by JNIEnv being !Send and !Sync but this
// assertion is added just to ensure that an unsafe implementation isn't added.
assert_not_impl_all!(Scope: Send, Sync);

#[derive(Debug, Clone)]
pub struct JavaEnv {
    vm: Arc<JavaVM>,
    resolver: MethodResolver,
}

impl JavaEnv {
    pub fn new(env: JNIEnv) -> JavaEnv {
        let vm = match env.get_java_vm() {
            Ok(vm) => vm,
            Err(_) => abort(),
        };
        JavaEnv {
            vm: Arc::new(vm),
            resolver: MethodResolver::default(),
        }
    }

    fn enter_scope(&self) -> Scope<'_> {
        debug!("Entering new scoped Java Environment");
        let JavaEnv { vm, resolver } = self;
        let env = match vm.get_env() {
            Ok(env) => {
                debug!("Found JNIEnv associated with the current thread");
                env
            }
            Err(Error::JniCall(JniError::ThreadDetached)) => {
                debug!("Current thread was not attached. Attempting to attach it as a daemon");
                match vm.attach_current_thread_as_daemon() {
                    Ok(env) => env,
                    Err(e) => {
                        error!(error = ?e, "Failed to attached the current thread as a daemon. Aborting");
                        abort()
                    }
                }
            }
            Err(_) => abort(),
        };

        Scope {
            vm: vm.clone(),
            env,
            resolver: resolver.clone(),
        }
    }

    pub fn initialise(&self, def: impl Into<JavaObjectMethodDef>) -> InitialisedJavaObjectMethod {
        let scope = self.enter_scope();
        self.resolver.resolve(&scope, def)
    }

    pub fn resolver(&self) -> MethodResolver {
        self.resolver.clone()
    }

    pub fn with_env<'e, F, R>(&'e self, exec: F) -> R
    where
        F: FnOnce(Scope<'e>) -> R,
    {
        let scope = self.enter_scope();
        exec(scope)
    }

    pub fn with_env_expect<'e, F, R, E>(&'e self, exec: F) -> R
    where
        F: FnOnce(Scope<'e>) -> Result<R, E>,
        E: StdError + Send + 'static,
    {
        let scope = self.enter_scope();
        match exec(scope) {
            Ok(o) => o,
            Err(e) => abort_vm(self.vm.clone(), e),
        }
    }

    pub fn with_env_throw<'e, 'c, F, R, E, T>(&'e self, class: T, exec: F) -> Result<R, ()>
    where
        F: FnOnce(Scope<'e>) -> Result<R, E>,
        E: StdError + Send + 'static,
        T: Desc<'e, JClass<'c>>,
    {
        let scope = self.enter_scope();
        match exec(scope.clone()) {
            Ok(o) => Ok(o),
            Err(e) => Err(scope.throw_new(class, e.to_string())),
        }
    }

    pub fn fatal_error<M>(&self, msg: M) -> !
    where
        M: ToString,
    {
        let scope = self.enter_scope();
        scope.fatal_error(msg)
    }
}

#[derive(Clone)]
pub struct Scope<'l> {
    vm: Arc<JavaVM>,
    env: JNIEnv<'l>,
    resolver: MethodResolver,
}

impl<'l> Scope<'l> {
    #[doc(hidden)]
    pub(crate) fn call_method_unchecked<H, O, T>(
        &self,
        def: &JavaObjectMethodDef,
        handler: &H,
        obj: O,
        method_id: T,
        ret: ReturnType,
        args: &[jvalue],
    ) -> Result<JValue<'l>, H::Err>
    where
        O: Into<JObject<'l>>,
        T: Desc<'l, JMethodID>,
        H: JavaExceptionHandler,
    {
        let method = format!("{}::{}", def.class(), def.name());
        debug!(
            def = method,
            signature = def.signature(),
            "Invoking object method"
        );

        let Scope { env, .. } = &self;
        fallible_jni_call(self.clone(), handler, || {
            env.call_method_unchecked(obj, method_id, ret, args)
        })
    }

    pub fn call_static_method<'c, T, U, V>(
        &self,
        class: T,
        name: U,
        sig: V,
        args: &[JValue],
    ) -> JValue<'l>
    where
        T: Desc<'l, JClass<'c>>,
        U: Into<JNIString>,
        V: Into<JNIString> + AsRef<str>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.call_static_method(class, name, sig, args))
    }

    pub fn throw_new<'c, S, T>(&self, class: T, msg: S)
    where
        S: Into<JNIString>,
        T: Desc<'l, JClass<'c>>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.throw_new(class, msg))
    }

    pub fn with_local_frame_null<F>(&self, effect: F)
    where
        F: FnOnce(),
    {
        let Scope { env, .. } = self;
        with_local_frame_null(env, None, effect)
    }

    pub(crate) fn get_method_id<'c, T, U, V>(&self, class: T, name: U, sig: V) -> JMethodID
    where
        T: Desc<'l, JClass<'c>>,
        U: Into<JNIString>,
        V: Into<JNIString>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.get_method_id(class, name, sig))
    }

    pub fn resolve(&self, def: impl Into<JavaObjectMethodDef>) -> InitialisedJavaObjectMethod {
        let Scope { resolver, .. } = &self;
        resolver.resolve(self, def)
    }

    pub fn set_field<O, S, T>(&self, obj: O, name: S, ty: T, val: JValue)
    where
        O: Into<JObject<'l>>,
        S: Into<JNIString>,
        T: Into<JNIString> + AsRef<str>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.set_field(obj, name, ty, val))
    }

    pub fn find_class<S>(&self, name: S) -> JClass<'l>
    where
        S: Into<JNIString>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.find_class(name))
    }

    pub fn get_object_class<O>(&self, obj: O) -> JClass<'l>
    where
        O: Into<JObject<'l>>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.get_object_class(obj))
    }

    pub fn initialise<D>(&self, def: D) -> InitialisedJavaObjectMethod
    where
        D: Into<JavaObjectMethodDef>,
    {
        let Scope { vm, .. } = self;
        system_call(vm, || def.into().initialise(self))
    }

    pub fn invoke<M, O>(&'l self, method: M, object: O, args: &[JValue<'l>]) -> M::Output
    where
        M: JavaObjectMethod<'l>,
        O: Into<JObject<'l>>,
    {
        let Scope { vm, env, .. } = self;
        jni_call(vm, env, || {
            let result = method.invoke(&AbortingHandler, self, object, args);
            match result {
                Ok(o) => Ok(o),
                Err(e) => match e {},
            }
        })
    }

    pub fn new_string<S>(&self, source: S) -> JString<'l>
    where
        S: Into<JNIString>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.new_string(source))
    }

    pub fn lock_obj<O>(&self, obj: O) -> MonitorGuard<'l>
    where
        O: Into<JObject<'l>>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.lock_obj(obj))
    }

    pub fn exception_occurred(&self) -> Option<JThrowable<'l>> {
        let Scope { vm, env, .. } = self;
        system_call(vm, || match env.exception_occurred() {
            Ok(throwable) => {
                let ptr = throwable.into_raw();
                if ptr.is_null() {
                    Ok(None)
                } else {
                    unsafe { Ok(Some(JThrowable::from_raw(ptr))) }
                }
            }
            Err(e) => Err(e),
        })
    }

    pub fn exception_describe(&self) {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.exception_describe())
    }

    pub fn exception_clear(&self) {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.exception_clear())
    }

    pub fn get_java_string<'s>(&'s self, obj: JString<'s>) -> JavaStr<'s, 'l>
    where
        'l: 's,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.get_string(obj))
    }

    pub fn get_rust_string(&self, obj: JString) -> String {
        let Scope { vm, env, .. } = self;
        let jstr = system_call(vm, || env.get_string(obj));
        String::from(jstr)
    }

    #[cold]
    #[inline(never)]
    pub fn fatal_error<M>(&self, msg: M) -> !
    where
        M: ToString,
    {
        let Scope { vm, .. } = self;
        abort_vm(vm.clone(), StringError(msg.to_string()))
    }

    pub fn new_global_ref<O>(&self, obj: O) -> GlobalRef
    where
        O: Into<JObject<'l>>,
    {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.new_global_ref(obj))
    }

    pub fn delete_local_ref(&self, obj: JObject) {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.delete_local_ref(obj))
    }

    pub fn convert_byte_array(&self, array: jbyteArray) -> Vec<u8> {
        let Scope { vm, env, .. } = self;
        system_call(vm, || env.convert_byte_array(array))
    }

    pub fn new_direct_byte_buffer_exact<'b, B>(&self, buf: &'b mut B) -> ByteBufferGuard<'b>
    where
        B: BufPtr,
        'l: 'b,
    {
        let Scope { vm, env, .. } = self;
        let buffer = system_call(vm, || unsafe {
            env.new_direct_byte_buffer(buf.as_mut_ptr(), buf.len())
        });
        ByteBufferGuard {
            _buf: Default::default(),
            buffer,
        }
    }

    pub unsafe fn new_direct_byte_buffer_global(
        &self,
        data: *mut u8,
        len: usize,
    ) -> GlobalRefByteBuffer {
        let Scope { vm, env, .. } = self;
        system_call(vm, || {
            let buffer = unsafe { env.new_direct_byte_buffer(data, len)? };
            let global_ref = env.new_global_ref(buffer)?;
            Ok(GlobalRefByteBuffer(global_ref))
        })
    }

    pub fn auto_local<'s, 'o>(&'s self, obj: JObject<'o>) -> AutoLocal<'o, 's>
    where
        's: 'o,
    {
        let Scope { env, .. } = self;
        AutoLocal::new(env, obj)
    }
}

#[must_use]
#[derive(Clone)]
pub struct GlobalRefByteBuffer(GlobalRef);

pub unsafe trait JObjectFromByteBuffer {
    fn as_byte_buffer(&self) -> JObject;
}

unsafe impl JObjectFromByteBuffer for GlobalRefByteBuffer {
    fn as_byte_buffer(&self) -> JObject {
        self.0.as_obj()
    }
}

/// Guard that binds the lifetime of the JByteBuffer to the backing data.
#[must_use]
#[derive(Clone, Copy)]
pub struct ByteBufferGuard<'b> {
    _buf: PhantomData<&'b mut Vec<u8>>,
    buffer: JByteBuffer<'b>,
}

impl<'b> From<ByteBufferGuard<'b>> for JValue<'b> {
    fn from(value: ByteBufferGuard<'b>) -> Self {
        unsafe { JValue::Object(JObject::from_raw(value.buffer.into_raw())) }
    }
}

unsafe impl<'b> JObjectFromByteBuffer for ByteBufferGuard<'b> {
    // This is unsafe due to the clone unbinding the object from the lifetime.
    fn as_byte_buffer(&self) -> JObject {
        *self.buffer.deref()
    }
}

#[derive(Debug)]
pub struct StringError(pub String);

impl StdError for StringError {}

impl Display for StringError {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        f.write_str(self.0.as_str())
    }
}

fn system_call<F, O>(vm: &Arc<JavaVM>, f: F) -> O
where
    F: FnOnce() -> Result<O, Error>,
{
    match f() {
        Ok(o) => o,
        Err(e) => match e {
            Error::JavaException => abort_unexpected_sys_exception(vm.clone()),
            e => abort_vm(vm.clone(), e),
        },
    }
}

fn get_env(vm: &JavaVM) -> Result<JNIEnv, ()> {
    match vm.get_env() {
        Ok(env) => Ok(env),
        Err(Error::JniCall(JniError::ThreadDetached)) => {
            vm.attach_current_thread_as_daemon().map_err(|_| ())
        }
        Err(_) => Err(()),
    }
}

#[cold]
#[inline(never)]
fn abort_unexpected_sys_exception(vm: Arc<JavaVM>) -> ! {
    const UNEXPECTED_EXCEPTION: &str =
        "An unexpected exception was thrown while making a system call";
    let backtrace = Backtrace::force_capture();

    if let Ok(env) = get_env(vm.as_ref()) {
        // This has to be done from the current thread as it contains the JniEnv that is associated
        // with the exception.
        let _r = env.exception_describe();
    }

    let _r = std::thread::spawn(move || {
        let _stdout_lock = std::io::stdout().lock();
        eprintln!("Aborting VM due to an unhandled exception during a system call");
        eprintln!("Stack backtrace:\n{backtrace}");

        if let Ok(env) = get_env(vm.as_ref()) {
            flush_output_streams(&env);

            eprintln!("{UNEXPECTED_EXCEPTION}");
            env.fatal_error(UNEXPECTED_EXCEPTION)
        } else {
            eprintln!("Failed to get Java VM");
        }
    })
    .join();
    abort()
}

/// Flushes Java output streams to ensure that any exception messages have been printed.
#[cold]
#[inline(never)]
fn flush_output_streams(env: &JNIEnv) {
    let r = env.call_static_method(
        "ai/swim/lang/ffi/ExceptionUtils",
        "flushOutputStreams",
        "()V",
        &[],
    );
    if r.is_err() {
        eprintln!("Failed to flush Java output streams");
    }
}

#[cold]
#[inline(never)]
fn abort_vm(vm: Arc<JavaVM>, e: impl StdError + Send + 'static) -> ! {
    let backtrace = Backtrace::force_capture();
    eprintln!("Aborting VM due to: {}\nStack backtrace: {}", e, backtrace);

    let _r = std::thread::spawn(move || {
        let _stdout_lock = std::io::stdout().lock();
        eprintln!("Stack backtrace:\n{backtrace}");

        if let Ok(env) = get_env(vm.as_ref()) {
            flush_output_streams(&env);
            eprintln!("Error executing Java call: {:?}", e);
            env.fatal_error(e.to_string())
        } else {
            eprintln!("Failed to get Java VM");
        }
    });
    abort()
}

/// Trait for molding a Java throwable to Rust types.
pub trait JavaExceptionHandler {
    type Err;

    /// Inspects 'throwable' and molds it to a 'Self::Err' if this handler is capable of handling
    /// it.
    ///
    /// # Note
    /// Prior to invoking this method, 'throwable' is obtained by a call to the JNIEnv's 'ExceptionOccurred'
    /// function which in some JVM implementations (E.g, Java 11 versions) has a side effect of
    /// clearing the exception; but this does not happen on Java 7. As such, implementors of this
    /// trait are left to print the stack trace associated with the throwable if it is required.
    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err>;
}

impl<E, F> JavaExceptionHandler for F
where
    F: Fn(&Scope, JThrowable) -> Option<E>,
{
    type Err = E;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        (self)(scope, throwable)
    }
}

#[derive(Debug)]
pub struct NotTypeOfExceptionHandler {
    method: InitialisedJavaObjectMethod,
    class: GlobalRef,
}

impl NotTypeOfExceptionHandler {
    pub fn new(env: &JavaEnv, class: &str) -> NotTypeOfExceptionHandler {
        let (class, method) = env.with_env(|scope| {
            let class = scope.new_global_ref(scope.find_class(class));
            (
                class,
                scope.resolve((
                    "java/lang/Class",
                    "isAssignableFrom",
                    "(Ljava/lang/Class;)Z",
                )),
            )
        });

        NotTypeOfExceptionHandler { method, class }
    }
}

impl JavaExceptionHandler for NotTypeOfExceptionHandler {
    type Err = SpannedError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        let NotTypeOfExceptionHandler { method, class } = self;
        let clazz = scope.get_object_class(throwable);
        let is_assignable_from = scope.invoke(method.z(), clazz, &[JValue::Object(class.as_obj())]);

        if is_assignable_from {
            None
        } else {
            Some(handle_exception(scope, Location::caller(), throwable))
        }
    }
}

struct AbortingHandler;

impl JavaExceptionHandler for AbortingHandler {
    type Err = Infallible;

    fn inspect(&self, scope: &Scope, _throwable: JThrowable) -> Option<Self::Err> {
        let _r = scope.exception_describe();

        abort_vm(
            scope.vm.clone(),
            StringError("An exception was not handled".into()),
        )
    }
}

#[derive(Debug)]
pub struct IsTypeOfExceptionHandler {
    method: InitialisedJavaObjectMethod,
    class: GlobalRef,
}

impl IsTypeOfExceptionHandler {
    pub fn new(env: &JavaEnv, class: &str) -> IsTypeOfExceptionHandler {
        let (class, method) = env.with_env(|scope| {
            let class = scope.new_global_ref(scope.find_class(class));
            (
                class,
                scope.resolve((
                    "java/lang/Class",
                    "isAssignableFrom",
                    "(Ljava/lang/Class;)Z",
                )),
            )
        });

        IsTypeOfExceptionHandler { method, class }
    }
}

impl JavaExceptionHandler for IsTypeOfExceptionHandler {
    type Err = SpannedError;

    fn inspect(&self, scope: &Scope, throwable: JThrowable) -> Option<Self::Err> {
        let IsTypeOfExceptionHandler { method, class } = self;
        let clazz = scope.get_object_class(throwable);
        let is_assignable_from = scope.invoke(method.z(), clazz, &[JValue::Object(class.as_obj())]);

        if is_assignable_from {
            Some(handle_exception(scope, Location::caller(), throwable))
        } else {
            None
        }
    }
}

/// A tracked Rust error that was caused by a Java method invocation that threw an exception. This
/// struct contains the callstack that led to the exception being thrown as well as a global
/// reference to the exception itself; which is freed when dropped.
#[derive(Debug)]
pub struct SpannedError {
    /// The Rust call site location that triggered the exception to be thrown.
    pub location: &'static Location<'static>,
    /// The Java exception stack trace converted to a string.
    pub stack_trace: String,
    /// The cause of the exception.
    pub cause: String,
    /// A global reference to the cause of the Java exception.
    pub cause_throwable: GlobalRef,
}

impl SpannedError {
    pub fn new(
        location: &'static Location<'static>,
        stack_trace: String,
        cause: String,
        cause_throwable: GlobalRef,
    ) -> SpannedError {
        SpannedError {
            location,
            stack_trace,
            cause,
            cause_throwable,
        }
    }
}

impl StdError for SpannedError {}

impl Display for SpannedError {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        let SpannedError {
            location,
            stack_trace,
            cause,
            ..
        } = self;

        writeln!(f, "Native exception thrown at call site: {location}")?;
        writeln!(f, "\tCaused by: {cause}")?;

        let stack_trace = stack_trace
            .lines()
            .map(|l| format!("\n\t\t{l}"))
            .collect::<String>();

        writeln!(f, "\tStack trace:{stack_trace}")
    }
}

#[inline(never)]
fn handle_exception(
    scope: &Scope,
    location: &'static Location,
    throwable: JThrowable,
) -> SpannedError {
    let cause_method = scope.resolve(Throwable::GET_CAUSE);
    let cause_global_ref = scope.new_global_ref(scope.invoke(
        cause_method.l(),
        unsafe { JObject::from_raw(throwable.into_raw()) },
        &[],
    ));
    let message_string = get_exception_message(scope, throwable);

    scope.exception_clear();

    let stack_trace_obj = scope.call_static_method(
        "ai/swim/lang/ffi/ExceptionUtils",
        "stackTraceString",
        "(Ljava/lang/Throwable;)Ljava/lang/String;",
        &[throwable.into()],
    );
    let stack_trace_str_obj = match stack_trace_obj.l() {
        Ok(obj) => JString::from(obj),
        Err(e) => abort_vm(scope.vm.clone(), e),
    };
    let stack_trace_string = scope.get_rust_string(stack_trace_str_obj);

    SpannedError::new(
        location,
        stack_trace_string,
        message_string,
        cause_global_ref,
    )
}

fn fallible_jni_call<O, F, I>(scope: Scope, exception_handler: &I, call: F) -> Result<O, I::Err>
where
    F: FnOnce() -> Result<O, jni::errors::Error>,
    I: JavaExceptionHandler,
{
    let Scope { env, vm, .. } = &scope;
    with_local_frame_null(env, None, || match (call)() {
        Ok(r) => Ok(r),
        Err(error) => match error {
            Error::JavaException => {
                // Safe as the JNI call returned 'Error::JavaException'.
                let throwable = scope.exception_occurred().expect("Missing throwable");

                // We cannot call 'ExceptionDescribe' here as some implementations have a side
                // effect of clearing the exception.

                scope.exception_clear();

                match exception_handler.inspect(&scope, throwable.clone()) {
                    Some(e) => Err(e),
                    None => {
                        error!("An exception was not handled by the provided handler. Aborting");

                        print_exception_stack_trace(&scope, &throwable);
                        abort_vm(vm.clone(), error)
                    }
                }
            }
            e => abort_vm(vm.clone(), e),
        },
    })
}

fn print_exception_stack_trace(scope: &Scope, throwable: &JThrowable) {
    if let Err(e) = scope
        .env
        .call_method(*throwable, "printStackTrace", "()V", &[])
    {
        error!(error = ?e, "Failed to print exception stack trace");
        abort_vm(scope.vm.clone(), e);
    }
}

fn jni_call<O, F>(vm: &Arc<JavaVM>, env: &JNIEnv, f: F) -> O
where
    F: FnOnce() -> Result<O, jni::errors::Error>,
{
    with_local_frame_null(env, None, || match (f)() {
        Ok(o) => o,
        Err(e) => abort_vm(vm.clone(), e),
    })
}

fn get_exception_message(scope: &Scope, throwable: JThrowable) -> String {
    let Scope { resolver, .. } = &scope;
    let method = resolver.resolve(&scope, Throwable::GET_MESSAGE);
    let message = scope.invoke(method.l(), throwable, &[]);

    scope.get_rust_string(JString::from(message))
}

#[derive(Debug, Clone, Default)]
pub struct MethodResolver {
    inner: Arc<Mutex<ResolverInner>>,
}

impl MethodResolver {
    pub fn resolve(
        &self,
        scope: &Scope,
        def: impl Into<JavaObjectMethodDef>,
    ) -> InitialisedJavaObjectMethod {
        let guard = &mut *self.inner.lock();
        guard.resolve(scope, def.into())
    }
}

#[derive(Debug, Default)]
struct ResolverInner {
    resolved: HashMap<JavaObjectMethodDef, InitialisedJavaObjectMethod>,
}

impl ResolverInner {
    fn resolve(&mut self, scope: &Scope, def: JavaObjectMethodDef) -> InitialisedJavaObjectMethod {
        debug!(method = ?def, "Resolving method");
        match self.resolved.entry(def) {
            Entry::Occupied(entry) => entry.get().clone(),
            Entry::Vacant(entry) => match def.initialise(scope) {
                Ok(init) => entry.insert(init).clone(),
                Err(e) => abort_vm(scope.vm.clone(), e),
            },
        }
    }
}

/// Executes a function inside while managing local references created during the execution. Once
/// the function has been executed, the popped local frame's return value is set to null.
fn with_local_frame_null<F, R>(env: &JNIEnv, capacity: Option<i32>, f: F) -> R
where
    F: FnOnce() -> R,
{
    env.push_local_frame(capacity.unwrap_or(jni::DEFAULT_LOCAL_FRAME_CAPACITY))
        .expect("Out of memory");

    let output = f();
    env.pop_local_frame(JObject::null())
        .expect("Failed to pop local reference frame");

    output
}

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
