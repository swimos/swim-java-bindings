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

use tokio::io::ReadBuf;

pub mod reader;
pub mod writer;

mod offset {
    use std::mem::size_of;

    #[cfg(target_endian = "little")]
    pub const CLOSED: usize = 0;
    #[cfg(target_endian = "big")]
    pub const CLOSED: usize = 3;
    pub const READ: usize = 1 * size_of::<i32>();
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

impl<'a> ReadTarget for ReadBuf<'a> {
    fn remaining(&self) -> usize {
        ReadBuf::remaining(self)
    }

    fn put_slice(&mut self, slice: &[u8]) {
        ReadBuf::put_slice(self, slice)
    }
}
