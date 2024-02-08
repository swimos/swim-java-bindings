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

use std::any::Any;
use std::io::Write;

use jni::JNIEnv;

pub const RUNTIME_EXCEPTION: &str = "java/lang/RuntimeException";

#[macro_export]
macro_rules! jni_try {
    ($env:ident, $class:expr, $msg:tt, $expr:expr, $ret:expr $(,)?) => {{
        let env_ref = $env;

        match $expr {
            Ok(val) => val,
            Err(e) => {
                env_ref
                    .throw_new($class, format!("{}: {:?}", $msg, e))
                    .expect("Failed to throw exception");
                return $ret;
            }
        }
    }};
    ($env:ident, $class:expr, $msg:tt, $expr:expr, $(,)?) => {{
        $crate::jni_try!($env, $class, $msg, $expr, ())
    }};
    ($env:ident, $msg:tt, $expr:expr $(,)?) => {
        $crate::jni_try!($env, $crate::RUNTIME_EXCEPTION, $msg, $expr, ())
    };
    ($env:ident, $msg:tt, $expr:expr, $ret:expr $(,)?) => {
        $crate::jni_try!(
            $env,
            $crate::RUNTIME_EXCEPTION,
            $msg,
            $expr,
            $ret
        )
    };
    ($expr:expr, $($ret:expr)? $(,)?) => {
        match $expr {
            Ok(()) => {}
            Err(()) => {
                return $($ret)?;
            }
        }
    };
    ($expr:expr $(,)?) => {
        $crate::jni_try!($expr, std::ptr::null_mut())
    };
}

/// Macro for defining functions that are invoked across an FFI boundary and need to be able to
/// catch an unwind and throw an exception instead of resuming the unwind. Ideally, crates that have
/// functions that will be invoked by Java, should reimplement this macro with the $exception_class
/// and possibly the Java package prefix arguments populated.
///
/// If a return type is specified, the function's return type will implicitly be set to be a pointer
/// type. If the inner function panics, then the unwind is caught and if the panic payload is of a
/// string type, then a Java exception is thrown with this message (with a class type of
/// $exception_type) and a default value is returned for the function.
#[macro_export]
macro_rules! ffi_fn {
    ($(#[$attrs:meta])*, $exception_class:tt, $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        #[no_mangle]
        $(#[$attrs])*
        pub extern "system" fn $name(
            $env:jni::JNIEnv,
            $class: jni::objects::JClass,
            $($arg:$ty,)*
        ) $(-> *mut $ret)? {
            match std::panic::catch_unwind(std::panic::AssertUnwindSafe(move || {
                $body
            })) {
                Ok(r) => r,
                Err(e) => $crate::throw(&$env, $exception_class, e)
            }
        }
    };
    ($(#[$attrs:meta])*, $exception_class:tt, $prefix:tt, $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        $crate::paste::item! {
            $(#[$attrs])*
            #[no_mangle]
            $(#[$attrs])*
            pub extern "system" fn [< $prefix _ $name >](
                $env:jni::JNIEnv,
                $class: jni::objects::JClass,
                $($arg:$ty,)*
            ) $(-> *mut $ret)? {
                match std::panic::catch_unwind(std::panic::AssertUnwindSafe(move || {
                    $body
                })) {
                    Ok(r) => r,
                    Err(e) => $crate::throw(&$env, $exception_class, e)
                }
            }
        }
    };
}

pub trait JniDefault {
    fn default() -> Self;
}

macro_rules! jni_default_impl {
    ($ty:ty, $val:tt) => {
        impl JniDefault for $ty {
            fn default() -> Self {
                $val
            }
        }
    };
}

jni_default_impl!((), ());
jni_default_impl!(jni::sys::jint, 0);
jni_default_impl!(jni::sys::jlong, 0);
jni_default_impl!(jni::sys::jbyte, 0);
jni_default_impl!(jni::sys::jboolean, 0);
jni_default_impl!(jni::sys::jchar, 0);
jni_default_impl!(jni::sys::jshort, 0);
jni_default_impl!(jni::sys::jfloat, 0.0);
jni_default_impl!(jni::sys::jdouble, 0.0);

impl<T> JniDefault for *mut T {
    fn default() -> Self {
        std::ptr::null_mut()
    }
}

/// Attempts to throw a Java exception of the provided class and with the cause displayed as a debug
/// string. This function cannot panic and instead if an error occurs then it prints the error
/// message and continues.
#[cold]
#[inline(never)]
pub fn throw<R>(env: &JNIEnv, class: &'static str, cause: Box<dyn Any + Send + 'static>) -> R
where
    R: JniDefault,
{
    fn describe<W, O, E>(writer: &mut W, result: Result<O, E>, on_err: &'static str)
    where
        W: Write,
    {
        if result.is_err() {
            let _r = write!(writer, "{on_err}");
        }
    }

    let out = std::io::stderr();
    let mut writer = out.lock();

    match env.exception_check() {
        Ok(true) => {
            let _r = writeln!(writer, "Unhandled exception. Printing and clearing it");
            describe(
                &mut writer,
                env.exception_describe(),
                "Failed to print exception\n",
            );
            describe(
                &mut writer,
                env.exception_clear(),
                "Failed to clear exception\n",
            );
        }
        Ok(false) => {}
        Err(e) => {
            let result = write!(
                writer,
                "Failed to check if an exception was being thrown: {e:?}"
            );

            if result.is_err() {
                env.fatal_error(result.unwrap_err().to_string());
            }
        }
    }

    let cause_str = if let Some(msg) = cause.as_ref().downcast_ref::<&str>() {
        msg.to_string()
    } else if let Some(msg) = cause.downcast_ref::<String>() {
        msg.to_string()
    } else {
        "(Unknown panic payload)".to_string()
    };

    describe(
        &mut writer,
        env.throw_new(class, cause_str),
        "Failed to throw exception",
    );

    R::default()
}
