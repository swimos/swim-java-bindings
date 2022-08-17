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

use std::fmt::Debug;
use std::io::Error;
use std::pin::Pin;
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::task::{Context, Poll};

use jni::objects::JByteBuffer;
use jni::sys::jobject;
use jni::JNIEnv;
use tokio::io::AsyncWrite;

use sys_util::jvm_tryf;

use crate::bytes::Bytes;
use crate::channel::{channel_io, Inner};
use crate::channel::{offset, WriteFailure};
use crate::vm::method::InvokeObjectMethod;
use crate::vm::method::JavaMethod;
use crate::vm::utils::get_env;

#[derive(Debug)]
pub struct ByteWriter {
    inner: Inner,
}

impl ByteWriter {
    pub fn new(env: JNIEnv, buffer: JByteBuffer, lock: jobject) -> ByteWriter {
        ByteWriter {
            inner: Inner::new(env, buffer, lock),
        }
    }

    /// Returns whether this writer is closed
    pub fn is_closed(&self) -> bool {
        self.inner.is_closed()
    }

    #[inline]
    pub fn try_write(&mut self, buf: &[u8]) -> Result<usize, WriteFailure> {
        let capacity = self.inner.data_capacity();
        let Inner {
            is_closed,
            read_index,
            write_index,
            lock,
            bytes,
            vm,
        } = &mut self.inner;

        match write(
            capacity,
            unsafe { is_closed.as_ref() },
            unsafe { read_index.as_ref() },
            unsafe { write_index.as_ref() },
            bytes,
            buf,
        ) {
            Ok(n) => {
                let lock_obj = lock.as_obj();
                let env = get_env(vm).expect("Failed to get JVM environment");
                let _guard = env.lock_obj(lock_obj).expect("Failed to enter monitor");

                jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock_obj, &[]));
                Ok(n)
            }
            Err(e) => Err(e),
        }
    }
}

#[inline(always)]
fn write(
    capacity: usize,
    is_closed: &AtomicBool,
    read_index: &AtomicI32,
    write_index: &AtomicI32,
    bytes: &mut Bytes,
    from: &[u8],
) -> Result<usize, WriteFailure> {
    if from.is_empty() {
        return Ok(0);
    }

    if is_closed.load(Ordering::Relaxed) {
        return Err(WriteFailure::Closed);
    }

    let write_offset = write_index.load(Ordering::Relaxed) as usize;
    let read_from = read_index.load(Ordering::Acquire) as usize;

    let mut remaining = read_from.wrapping_sub(write_offset + 1);
    remaining = remaining % capacity + 1;

    if remaining <= 1 {
        return Err(WriteFailure::Full);
    }

    let to_write = from.len().min(remaining - 1);
    let capped = to_write.min(capacity - write_offset);

    bytes.copy_from_slice(offset::DATA_START + write_offset, &from[..capped]);

    let mut new_write_offset = if capped != to_write {
        let lim = to_write - capped;
        bytes.copy_from_slice(offset::DATA_START, &from[capped..(capped + lim)]);

        lim
    } else {
        write_offset + to_write
    };
    if new_write_offset == capacity {
        new_write_offset = 0;
    }

    write_index.store(new_write_offset as i32, Ordering::Release);

    Ok(to_write)
}

impl AsyncWrite for ByteWriter {
    fn poll_write(
        mut self: Pin<&mut Self>,
        _cx: &mut Context<'_>,
        buf: &[u8],
    ) -> Poll<Result<usize, Error>> {
        let capacity = self.inner.data_capacity();
        let Inner {
            is_closed,
            read_index,
            write_index,
            lock,
            bytes,
            vm,
        } = &mut self.as_mut().get_mut().inner;

        channel_io(lock.as_obj(), vm, || {
            write(
                capacity,
                unsafe { is_closed.as_ref() },
                unsafe { read_index.as_ref() },
                unsafe { write_index.as_ref() },
                bytes,
                buf,
            )
        })
    }

    fn poll_flush(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), Error>> {
        Poll::Ready(Ok(()))
    }

    fn poll_shutdown(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<Result<(), Error>> {
        let env = get_env(&self.inner.vm).expect("Failed to get JVM environment");
        let _guard = env
            .lock_obj(&self.inner.lock)
            .expect("Failed to enter monitor");

        jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, &self.inner.lock, &[]));
        Poll::Ready(Ok(()))
    }
}
