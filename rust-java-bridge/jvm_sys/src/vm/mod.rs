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

use std::error::Error;
use std::fmt::Display;
use std::panic::{panic_any, Location};
use std::{fmt, panic};

use jni::errors::JniError;
use jni::objects::{JObject, JString};
use jni::JNIEnv;
use std::io::Write;

pub mod exception;
pub mod method;
pub mod utils;

enum ErrorDiscriminate {
    Exception,
    Detached,
    Bug,
}

impl From<&jni::errors::Error> for ErrorDiscriminate {
    fn from(d: &jni::errors::Error) -> ErrorDiscriminate {
        match d {
            jni::errors::Error::JavaException => ErrorDiscriminate::Exception,
            jni::errors::Error::JniCall(JniError::ThreadDetached) => ErrorDiscriminate::Detached,
            _ => ErrorDiscriminate::Bug,
        }
    }
}

#[cfg(windows)]
const LINE_ENDING: &'static str = "\r\n";
#[cfg(not(windows))]
const LINE_SEPARATOR: &'static str = "\n";

#[derive(Debug)]
struct StringError(String);

impl Error for StringError {
    fn description(&self) -> &str {
        &self.0
    }
}

impl Display for StringError {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        Display::fmt(&self.0, f)
    }
}

#[derive(Debug)]
pub struct SpannedError {
    pub location: &'static Location<'static>,
    pub stack_trace: String,
    pub cause: Box<dyn Error + Send + Sync>,
}

impl SpannedError {
    pub fn new<C>(
        location: &'static Location<'static>,
        stack_trace: String,
        cause: C,
    ) -> SpannedError
    where
        C: Error + Send + Sync + 'static,
    {
        SpannedError {
            location,
            stack_trace,
            cause: Box::new(cause),
        }
    }

    pub fn stack_trace_header(&self) -> &str {
        match self.stack_trace.find(LINE_SEPARATOR) {
            Some(idx) => &self.stack_trace[..idx],
            None => &self.stack_trace,
        }
    }

    #[cold]
    #[inline(never)]
    pub fn panic(self) -> ! {
        panic_any(self)
    }
}

#[derive(Debug)]
pub enum InvocationError {
    Spanned(SpannedError),
    Detached,
}

#[cfg_attr(debug_assertions, track_caller)]
pub fn jni_call<O, F>(env: &JNIEnv, mut f: F) -> Result<O, SpannedError>
where
    F: FnMut() -> Result<O, jni::errors::Error>,
{
    match (f)() {
        Ok(o) => Ok(o),
        Err(e) => match ErrorDiscriminate::from(&e) {
            ErrorDiscriminate::Exception => Err(handle_exception(Location::caller(), env)),
            ErrorDiscriminate::Bug => {
                panic!("Failed to execute JNI function. Cause: {:?}", e)
            }
            ErrorDiscriminate::Detached => {
                unreachable!("Attempted to use a detached JNI interface")
            }
        },
    }
}

#[inline(never)]
fn handle_exception(location: &'static Location, env: &JNIEnv) -> SpannedError {
    const EXCEPTION_MSG: &'static str = "Failed to get exception message";
    const STACK_TRACE_MSG: &'static str = "Failed to get stacktrace";

    let throwable = env.exception_occurred().expect(EXCEPTION_MSG);

    // We want to clear the exception here, yield the error to the runtime for it to
    // determine whether to abort the runtime or close the downlink.
    env.exception_clear().expect("Failed to clear exception");

    let message = env
        .call_method(throwable, "getMessage", "()Ljava/lang/String;", &[])
        .expect(EXCEPTION_MSG);

    let message_string = match message.l() {
        Ok(obj) => match env.get_string(JString::from(obj)) {
            Ok(java_str) => java_str.to_str().expect(STACK_TRACE_MSG).to_string(),
            Err(jni::errors::Error::NullPtr(_)) => "".to_string(),
            Err(e) => {
                panic!("{}: {:?}", EXCEPTION_MSG, e)
            }
        },
        Err(_) => "".to_string(),
    };

    let stack_trace_obj = env
        .call_static_method(
            "ai/swim/client/Utils",
            "stackTraceString",
            "(Ljava/lang/Throwable;)Ljava/lang/String;",
            &[throwable.into()],
        )
        .expect(STACK_TRACE_MSG);
    let stack_trace_string = env
        .get_string(JString::from(stack_trace_obj.l().expect(STACK_TRACE_MSG)))
        .expect(STACK_TRACE_MSG);

    SpannedError::new(
        location,
        stack_trace_string.into(),
        StringError(message_string),
    )
}

pub fn set_panic_hook() {
    let existing_hook = panic::take_hook();
    panic::set_hook(Box::new(move |info| {
        let payload = &info.payload();
        if payload.is::<SpannedError>() {
            let SpannedError {
                location,
                stack_trace,
                cause,
            } = payload
                .downcast_ref::<SpannedError>()
                .expect("Failed to downcast to SpannedError");

            let thread = std::thread::current();
            let name = thread.name().unwrap_or("<unnamed>");
            let mut out = std::io::stderr();

            let cause = format!("{}", cause);
            let cause_fmt = if cause.is_empty() {
                "".to_string()
            } else {
                format!(" '{}'", cause)
            };

            let _lock = out.lock();

            if stack_trace.is_empty() {
                let _r = writeln!(
                    out,
                    "thread '{name}' panicked at JNI call{cause_fmt}, {location}"
                );
            } else {
                let _r = writeln!(out, "thread '{name}' panicked at JNI call{cause_fmt}, {location}, stack trace:\n\t{stack_trace}");
            }
        } else {
            existing_hook(info)
        }
    }));
}

pub fn with_local_frame_null<F, R>(env: &JNIEnv, capacity: Option<i32>, f: F) -> R
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
