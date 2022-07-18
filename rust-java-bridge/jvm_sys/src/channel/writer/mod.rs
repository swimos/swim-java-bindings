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

use std::fmt::{Debug, Formatter};
use std::io::{Error, ErrorKind};
use std::pin::Pin;
use std::sync::atomic::{AtomicPtr, Ordering};
use std::task::{Context, Poll};

use jni::objects::{GlobalRef, JByteBuffer};
use jni::sys::jobject;
use jni::JNIEnv;
use jni::JavaVM;
use tokio::io::AsyncWrite;

use sys_util::jvm_tryf;

use crate::bytes::Bytes;
use crate::channel::{offset, WriteFailure};
use crate::vm::method::InvokeObjectMethod;
use crate::vm::method::JavaMethod;
use crate::vm::utils::get_env;

pub struct ByteWriter {
    is_closed: AtomicPtr<bool>,
    lock: GlobalRef,
    bytes: Bytes,
    vm: JavaVM,
}

impl Debug for ByteWriter {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ByteWriter")
            .field("is_closed", &self.is_closed.load(Ordering::Relaxed))
            .field("bytes", &"...")
            .field("vm", &"..")
            .finish()
    }
}

unsafe impl Send for ByteWriter {}
unsafe impl Sync for ByteWriter {}

impl ByteWriter {
    pub fn new(env: JNIEnv, buffer: JByteBuffer, lock: jobject) -> ByteWriter {
        let bytes = Bytes::new(env, buffer);
        let global_ref = env
            .new_global_ref(lock)
            .expect("Failed to get a global reference to byte channel lock");
        let reader = ByteWriter {
            is_closed: AtomicPtr::new(unsafe { bytes.get_mut_u8(offset::CLOSED) } as *mut bool),
            lock: global_ref,
            bytes,
            vm: jvm_tryf!(env, env.get_java_vm()),
        };

        reader
    }

    /// Returns whether this writer is closed
    pub fn is_closed(&self) -> bool {
        unsafe { *self.is_closed.load(Ordering::Relaxed) }
    }

    fn data_capacity(&self) -> usize {
        self.bytes.capacity() - offset::DATA_START
    }
}

fn write(
    capacity: usize,
    is_closed: &AtomicPtr<bool>,
    bytes: &mut Bytes,
    from: &[u8],
) -> Result<usize, WriteFailure> {
    if from.is_empty() {
        return Ok(0);
    }

    if unsafe { *is_closed.load(Ordering::Relaxed) } {
        return Err(WriteFailure::Closed);
    }

    let read_from = bytes.get_i32(offset::READ) as usize;
    let write_offset = bytes.get_i32(offset::WRITE) as usize;

    let mut remaining = read_from.wrapping_sub(write_offset + 1);
    remaining = remaining % capacity + 1;

    if remaining <= 1 {
        return Err(WriteFailure::Full);
    }

    let to_write = from.len().min(remaining - 1);
    let capped = to_write.min(capacity - write_offset);

    unsafe {
        bytes.copy_from_slice(offset::DATA_START + write_offset, &from[..capped]);
    }

    let mut new_write_offset = if capped != to_write {
        let lim = to_write - capped;
        unsafe {
            bytes.copy_from_slice(offset::DATA_START, &from[capped..(capped + lim)]);
        }

        lim
    } else {
        write_offset + to_write
    };
    if new_write_offset == capacity {
        new_write_offset = 0;
    }

    bytes.set_i32(offset::WRITE, new_write_offset as i32);

    Ok(to_write)
}

impl AsyncWrite for ByteWriter {
    fn poll_write(
        mut self: Pin<&mut Self>,
        _cx: &mut Context<'_>,
        buf: &[u8],
    ) -> Poll<Result<usize, Error>> {
        let capacity = self.data_capacity();

        let ByteWriter {
            is_closed,
            lock,
            bytes,
            vm,
        } = self.as_mut().get_mut();

        let lock_obj = lock.as_obj();
        let env = get_env(&vm).expect("Failed to get JVM environment");
        let _guard = env.lock_obj(lock_obj).expect("Failed to enter monitor");

        loop {
            match write(capacity, is_closed, bytes, buf) {
                Ok(n) => {
                    jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock_obj, &[]));
                    return Poll::Ready(Ok(n));
                }
                Err(WriteFailure::Full) => {
                    jvm_tryf!(env, JavaMethod::WAIT.invoke(&env, lock_obj, &[]));
                }
                Err(WriteFailure::Closed) => break Poll::Ready(Err(ErrorKind::BrokenPipe.into())),
            }
        }
    }

    fn poll_flush(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), Error>> {
        Poll::Ready(Ok(()))
    }

    fn poll_shutdown(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), Error>> {
        let env = get_env(&self.vm).expect("Failed to get JVM environment");
        let _guard = env.lock_obj(&self.lock).expect("Failed to enter monitor");

        jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, &self.lock, &[]));
        Poll::Ready(Ok(()))
    }
}
