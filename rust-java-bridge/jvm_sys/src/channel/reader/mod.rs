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
use std::pin::Pin;
use std::ptr::NonNull;
use std::sync::atomic::{AtomicBool, Ordering};
use std::task::{Context, Poll};

use jni::objects::{GlobalRef, JByteBuffer};
use jni::sys::jobject;
use jni::{JNIEnv, JavaVM};
use tokio::io::{AsyncRead, ReadBuf};

use sys_util::jvm_tryf;

use crate::bytes::Bytes;
use crate::channel::{channel_io, drop_end, offset, ReadFailure, ReadTarget};
use crate::vm::method::InvokeObjectMethod;
use crate::vm::method::JavaMethod;
use crate::vm::utils::get_env;

pub struct ByteReader {
    is_closed: NonNull<AtomicBool>,
    lock: GlobalRef,
    bytes: Bytes,
    vm: JavaVM,
}

impl Debug for ByteReader {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ByteReader")
            .field("is_closed", unsafe {
                &self.is_closed.as_ref().load(Ordering::Relaxed)
            })
            .field("bytes", &"...")
            .field("vm", &"..")
            .finish()
    }
}

unsafe impl Send for ByteReader {}

unsafe impl Sync for ByteReader {}

impl ByteReader {
    pub fn new(env: JNIEnv, buffer: JByteBuffer, lock: jobject) -> ByteReader {
        let bytes = Bytes::new(env, buffer);
        let global_ref = env
            .new_global_ref(lock)
            .expect("Failed to get a global reference to byte channel lock");
        let reader = ByteReader {
            is_closed: unsafe { NonNull::new_unchecked(bytes.get_mut_u8(offset::CLOSED) as _) },
            lock: global_ref,
            bytes,
            vm: jvm_tryf!(env, env.get_java_vm()),
        };

        reader
    }

    /// Returns whether this reader is closed
    #[inline]
    pub fn is_closed(&self) -> bool {
        unsafe { self.is_closed.as_ref().load(Ordering::Relaxed) }
    }

    #[inline]
    pub fn try_read<R>(&mut self, read_target: &mut R) -> Result<(), ReadFailure>
    where
        R: ReadTarget,
    {
        let capacity = self.data_capacity();
        let ByteReader {
            is_closed,
            lock,
            bytes,
            vm,
        } = self;

        let lock_obj = lock.as_obj();
        let env = get_env(&vm).expect("Failed to get JVM environment");
        let _guard = env.lock_obj(lock_obj).expect("Failed to enter monitor");

        match read(capacity, unsafe { is_closed.as_ref() }, bytes, read_target) {
            Ok(()) => {
                jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, lock_obj, &[]));
                Ok(())
            }
            Err(e) => Err(e),
        }
    }

    fn data_capacity(&self) -> usize {
        self.bytes.capacity() - offset::DATA_START
    }
}

impl Drop for ByteReader {
    fn drop(&mut self) {
        drop_end(&self.is_closed, &self.vm, &self.lock);
    }
}

#[inline(always)]
fn read<R>(
    capacity: usize,
    is_closed: &AtomicBool,
    bytes: &mut Bytes,
    read_target: &mut R,
) -> Result<(), ReadFailure>
where
    R: ReadTarget,
{
    if read_target.remaining() == 0 {
        return Ok(());
    }

    let read_from = bytes.get_i32(offset::READ) as usize;
    let write_offset = bytes.get_i32(offset::WRITE) as usize;
    let available = write_offset.wrapping_sub(read_from) % capacity;

    if available == 0 {
        return if is_closed.load(Ordering::Relaxed) {
            Err(ReadFailure::Closed)
        } else {
            Err(ReadFailure::Empty)
        };
    }

    let to_read = available.min(read_target.remaining());
    let capped = to_read.min(capacity - read_from);

    unsafe {
        let data_slice = bytes.slice(offset::DATA_START + read_from, capped);
        read_target.put_slice(&*data_slice);
    }

    let mut new_read_offset = if capped != to_read {
        let lim = to_read - capped;
        unsafe {
            let data_slice = bytes.slice(offset::DATA_START, lim);
            read_target.put_slice(&*data_slice);
        }

        lim
    } else {
        read_from + to_read
    };

    if new_read_offset == capacity {
        new_read_offset = 0;
    }

    bytes.set_i32(offset::READ, new_read_offset as i32);

    Ok(())
}

impl AsyncRead for ByteReader {
    fn poll_read(
        mut self: Pin<&mut Self>,
        _cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<std::io::Result<()>> {
        let capacity = self.data_capacity();
        let ByteReader {
            is_closed,
            lock,
            bytes,
            vm,
        } = self.as_mut().get_mut();

        channel_io(lock.as_obj(), &vm, || {
            read(capacity, unsafe { is_closed.as_ref() }, bytes, buf)
        })
    }
}
