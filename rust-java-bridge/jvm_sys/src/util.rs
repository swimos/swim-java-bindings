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

use jni::JNIEnv;
use std::panic::Location;

/// Performs a null pointer check on all the pointers provided and if any is null then the JVM
/// aborts.
#[macro_export]
macro_rules! null_pointer_check_abort {
    ($env:ident, $($arg:ident),*) => {
        let env_ref = $env;
        $(
            if $arg.is_null() {
                $crate::util::abort_npe(&env_ref);
            }
        )*
    };
}

/// Performs a null pointer check on all the pointers provided and if any is null then a null
/// pointer is returned.
#[macro_export]
macro_rules! null_pointer_check {
    ($env:ident, $($arg:ident),*) => {
        let env_ref = $env;
        $(
            if $arg.is_null() {
                return std::ptr::null();
            }
        )*
    };
}

#[cold]
#[track_caller]
#[inline(never)]
pub fn abort_npe(env: &JNIEnv) -> ! {
    let caller = Location::caller();
    let message = format!("Null pointer: {}", caller);
    env.fatal_error(message)
}

/// Executes an expression that returns Result<O,E> and if in the error variant then the JVM is
/// aborted with either the string representation of the error variant or a user-provided message.
#[macro_export]
macro_rules! jvm_tryf {
    ($env:ident, $body:expr) => {{
        let env_ref = $env;
        match $body {
            Ok(o) => o,
            Err(e) => env_ref.fatal_error(e.to_string()),
        }
    }};
    ($env:ident, $msg:expr, $body:expr) => {{
        let env_ref = $env;
        match $body {
            Ok(o) => o,
            Err(_) => env_ref.fatal_error($msg),
        }
    }};
}
