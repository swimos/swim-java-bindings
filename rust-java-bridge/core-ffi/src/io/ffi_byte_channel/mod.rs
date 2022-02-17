// Copyright 2015-2021 Swim Inc.
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

use crate::java::{InvokeObjectMethod, METHOD_OBJECT_NOTIFY};
use jni::errors::Result as JniResult;
use jni::objects::GlobalRef;
use jni::{JNIEnv, JavaVM};
use std::fmt::Debug;
use std::task::Waker;

pub mod jreader_rwriter;
pub mod jwriter_rreader;

enum Waiter {
    None,
    Reader(Option<Waker>),
    Writer(Option<GlobalRef>),
}

#[inline]
fn get_env(vm: &JavaVM) -> JniResult<JNIEnv> {
    match vm.get_env() {
        Ok(env) => Ok(env),
        Err(jni::errors::Error::JniCall(jni::errors::JniError::ThreadDetached)) => {
            vm.attach_current_thread_as_daemon()
        }
        Err(e) => Err(e),
    }
}

impl Waiter {
    #[inline]
    fn wake(&mut self, vm: &JavaVM) -> JniResult<()> {
        match self {
            Waiter::Reader(opt) => {
                //println!("Waiter::Reader: {}", opt.is_some());
                if let Some(waker) = opt.take() {
                    waker.wake();
                }

                Ok(())
            }
            Waiter::Writer(lock) => {
                //println!("Waiter::Writer: {}", lock.is_some());

                if let Some(lock) = lock.take() {
                    let env = get_env(&vm)?;
                    let _monitor = env.lock_obj(&lock)?;

                    METHOD_OBJECT_NOTIFY.invoke(&env, lock.as_obj(), &[])?;

                    // The global reference and monitor will be freed when it's dropped
                }

                Ok(())
            }
            Waiter::None => {
                //println!("Waiter::None")
                Ok(())
            }
        }
    }
}

#[cold]
#[inline(never)]
fn abort(env: &JNIEnv, cause: impl Debug) -> ! {
    env.exception_describe().expect("JNI error");
    env.fatal_error(format!("JNI error: {:?}", cause));
}
