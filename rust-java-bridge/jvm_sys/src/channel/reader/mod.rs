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
use std::io::ErrorKind;
use std::pin::Pin;
use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
use std::task::{Context, Poll};

use jni::objects::JByteBuffer;
use jni::sys::jobject;
use jni::JNIEnv;
use tokio::io::{AsyncRead, ReadBuf};

use sys_util::jvm_tryf;

use crate::bytes::Bytes;
use crate::channel::{offset, Inner, ReadFailure, ReadTarget};
use crate::vm::method::InvokeObjectMethod;
use crate::vm::method::JavaMethod;
use crate::vm::utils::get_env;

#[derive(Debug)]
pub struct ByteReader {
    inner: Inner,
}

impl ByteReader {
    pub fn new(env: JNIEnv, buffer: JByteBuffer, lock: jobject) -> ByteReader {
        ByteReader {
            inner: Inner::new(env, buffer, lock),
        }
    }

    /// Returns whether this reader is closed
    #[inline]
    pub fn is_closed(&self) -> bool {
        self.inner.is_closed()
    }

    #[inline]
    pub fn try_read<R>(&mut self, read_target: &mut R) -> Result<(), ReadFailure>
    where
        R: ReadTarget,
    {
        let capacity = self.inner.data_capacity();
        let Inner {
            is_closed,
            write_index,
            read_index,
            lock,
            bytes,
            vm,
        } = &mut self.inner;

        let read_result = read(
            capacity,
            unsafe { is_closed.as_ref() },
            unsafe { write_index.as_ref() },
            unsafe { read_index.as_ref() },
            bytes,
            read_target,
        );

        let lock_obj = lock.as_obj();
        let env = get_env(vm).expect("Failed to get JVM environment");
        let _guard = env.lock_obj(lock_obj).expect("Failed to enter monitor");

        jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock_obj, &[]));

        read_result
    }
}

#[inline(always)]
fn read<R>(
    capacity: usize,
    is_closed: &AtomicBool,
    write_index: &AtomicI32,
    read_index: &AtomicI32,
    bytes: &mut Bytes,
    read_target: &mut R,
) -> Result<(), ReadFailure>
where
    R: ReadTarget,
{
    if read_target.remaining() == 0 {
        return Ok(());
    }

    let mut read_from = read_index.load(Ordering::Relaxed) as usize;
    let mut write_offset = write_index.load(Ordering::Acquire) as usize;
    let mut available = write_offset.wrapping_sub(read_from) % capacity;

    if available == 0 {
        return if is_closed.load(Ordering::Acquire) {
            read_from = read_index.load(Ordering::Relaxed) as usize;
            write_offset = write_index.load(Ordering::Acquire) as usize;
            available = write_offset.wrapping_sub(read_from) % capacity;

            if available != 0 {
                return read(
                    capacity,
                    is_closed,
                    write_index,
                    read_index,
                    bytes,
                    read_target,
                );
            }

            Err(ReadFailure::Closed)
        } else {
            Err(ReadFailure::Empty)
        };
    }

    let to_read = available.min(read_target.remaining());
    let capped = to_read.min(capacity - read_from);

    unsafe {
        let data_slice = bytes.slice_mut(offset::DATA_START + read_from, capped);
        read_target.put_slice(&*data_slice);
    }

    let mut new_read_offset = if capped != to_read {
        let lim = to_read - capped;
        unsafe {
            let data_slice = bytes.slice_mut(offset::DATA_START, lim);
            read_target.put_slice(&*data_slice);
        }

        lim
    } else {
        read_from + to_read
    };

    if new_read_offset == capacity {
        new_read_offset = 0;
    }

    read_index.store(new_read_offset as i32, Ordering::Release);

    Ok(())
}

impl AsyncRead for ByteReader {
    fn poll_read(
        mut self: Pin<&mut Self>,
        _cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<std::io::Result<()>> {
        let capacity = self.inner.data_capacity();
        let Inner {
            is_closed,
            write_index,
            read_index,
            lock,
            bytes,
            vm,
        } = &mut self.as_mut().get_mut().inner;

        let env = get_env(vm).expect("Failed to get JVM environment");
        let is_closed = unsafe { is_closed.as_ref() };
        let write_index = unsafe { write_index.as_ref() };
        let read_index = unsafe { read_index.as_ref() };

        loop {
            match read(capacity, is_closed, write_index, read_index, bytes, buf) {
                Ok(()) => {
                    let _guard = env
                        .lock_obj(lock.as_obj())
                        .expect("Failed to enter monitor");
                    jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock.as_obj(), &[]));

                    break Poll::Ready(Ok(()));
                }
                Err(ReadFailure::Empty) => {
                    let _guard = env
                        .lock_obj(lock.as_obj())
                        .expect("Failed to enter monitor");

                    let read_from = read_index.load(Ordering::Relaxed) as usize;
                    let write_offset = write_index.load(Ordering::Acquire) as usize;
                    let available = write_offset.wrapping_sub(read_from) % capacity;

                    if available == 0 {
                        jvm_tryf!(env, JavaMethod::WAIT.invoke(&env, lock.as_obj(), &[]));
                    }
                }
                Err(ReadFailure::Closed) => break Poll::Ready(Err(ErrorKind::BrokenPipe.into())),
            }
        }
    }
}
