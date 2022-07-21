// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! JNI boundary byte channels.
//!
//! # Layout/synchronization
//! A channel is backed by a single buffer that contains a closed flag (u32), read index (u32),
//! write index (u32) and then data (u8). This results in 12 extra bytes being required in addition
//! to the requested channel capacity by the user. Only operations on the closed flag are atomic
//! and all other get/set operations are done behind synchronization using Java monitors.
//!
//! On both the Java and Rust side, when a reader/writer wants to perform an operation a monitor
//! must be acquired on the provided lock object before any access is granted to the shared buffer.
//! Following the operation taking place, the lock object is notified and the monitor is
//! relinquished.
//!
//! For Java this is:
//! ```ignore
//! synchronized(lock) {
//!     // do buffer work
//!     lock.notify();
//! }
//! ```
//!
//! For Rust this is:
//! ```ignore
//! let monitor = env.lock_obj(&self.lock).expect("Failed to enter monitor");
//! // do buffer work
//! jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, &self.lock, &[]));
//! // monitor is released when it is dropped
//! ```

use std::io::ErrorKind;
use std::ptr::NonNull;
use std::sync::atomic::{AtomicBool, Ordering};
use std::task::Poll;

use jni::objects::{GlobalRef, JObject};
use jni::JavaVM;
use tokio::io::ReadBuf;

use sys_util::jvm_tryf;

use crate::vm::method::InvokeObjectMethod;
use crate::vm::method::JavaMethod;
use crate::vm::utils::get_env;

pub mod reader;
pub mod writer;

mod offset {
    use std::mem::size_of;

    #[cfg(target_endian = "little")]
    pub const CLOSED: usize = 0;
    #[cfg(target_endian = "big")]
    pub const CLOSED: usize = 3;
    pub const READ: usize = size_of::<i32>();
    pub const WRITE: usize = 2 * size_of::<i32>();
    pub const DATA_START: usize = 3 * size_of::<i32>();
}

pub trait ReadTarget {
    fn remaining(&self) -> usize;

    fn put_slice(&mut self, slice: &[u8]);
}

#[derive(Debug)]
pub enum WriteFailure {
    Closed,
    Full,
}

#[derive(Debug)]
pub enum ReadFailure {
    Closed,
    Empty,
}

trait ErrorStatus {
    fn status(&self) -> Status;
}

enum Status {
    FullOrEmpty,
    Closed,
}

impl ErrorStatus for WriteFailure {
    fn status(&self) -> Status {
        match self {
            WriteFailure::Closed => Status::Closed,
            WriteFailure::Full => Status::FullOrEmpty,
        }
    }
}

impl ErrorStatus for ReadFailure {
    fn status(&self) -> Status {
        match self {
            ReadFailure::Closed => Status::Closed,
            ReadFailure::Empty => Status::FullOrEmpty,
        }
    }
}

impl<'a> ReadTarget for ReadBuf<'a> {
    fn remaining(&self) -> usize {
        ReadBuf::remaining(self)
    }

    fn put_slice(&mut self, slice: &[u8]) {
        ReadBuf::put_slice(self, slice)
    }
}

fn drop_end(is_closed: &NonNull<AtomicBool>, vm: &JavaVM, lock: &GlobalRef) {
    unsafe { is_closed.as_ref().store(true, Ordering::Relaxed) };

    let env = get_env(vm).expect("Failed to get JVM environment");
    let _guard = env.lock_obj(lock).expect("Failed to enter monitor");

    jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock, &[]));
}

fn channel_io<F, O, E, E2>(lock: JObject, vm: &JavaVM, mut f: F) -> Poll<Result<O, E2>>
where
    F: FnMut() -> Result<O, E>,
    E: ErrorStatus,
    E2: From<ErrorKind>,
{
    let env = get_env(vm).expect("Failed to get JVM environment");
    let _guard = env.lock_obj(lock).expect("Failed to enter monitor");

    loop {
        match f() {
            Ok(o) => {
                println!("Rust: read notifying");
                jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock, &[]));
                println!("Rust: read notified");
                break Poll::Ready(Ok(o));
            }
            Err(e) => match e.status() {
                Status::FullOrEmpty => {
                    println!("Rust: read waiting");
                    jvm_tryf!(env, JavaMethod::WAIT.invoke(&env, lock, &[]));
                    println!("Rust: read waited");
                }
                Status::Closed => break Poll::Ready(Err(ErrorKind::BrokenPipe.into())),
            },
        }
    }
}
