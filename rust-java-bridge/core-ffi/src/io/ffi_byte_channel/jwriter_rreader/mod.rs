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

use std::fmt::Debug;
use std::pin::Pin;
use std::sync::Arc;
use std::task::{Context, Poll};

use crate::io::ffi_byte_channel::{abort, get_env, JniResult, Waiter};
use crate::java::{InvokeObjectMethod, JavaException, ThrowException, METHOD_OBJECT_WAIT};
use bytes::{Buf, BytesMut};
use jni::errors::{Error, JniError};
use jni::objects::{JClass, JObject};
use jni::sys::{jbyteArray, jint};
use jni::{JNIEnv, JavaVM};
use parking_lot::Mutex;
use std::io::Result as IoResult;
use tokio::io::{AsyncRead, AsyncReadExt, ReadBuf};

use crate::runtime::get_runtime;

const WRITE_EXCEPTION: JavaException =
    JavaException::new("ai/swim/io/WriteException", "ByteWriter channel closed");

struct Conduit {
    data: BytesMut,
    capacity: usize,
    vm: Arc<JavaVM>,
    closed: bool,
    waiter: Waiter,
}

impl Conduit {
    #[inline]
    fn close_channel(&mut self) -> JniResult<()> {
        self.closed = true;
        self.wake()
    }

    #[inline]
    fn wake(&mut self) -> JniResult<()> {
        self.waiter.wake(&self.vm)
    }

    #[inline]
    fn read(&mut self, buf: &mut ReadBuf<'_>, count: usize) -> JniResult<()> {
        debug_assert!(buf.remaining() > 0);
        debug_assert!(count > 0);

        buf.put_slice(&self.data[..count]);
        self.data.advance(count);
        self.wake()
    }

    #[inline]
    fn write(&mut self, buf: &[u8], avail: usize) -> JniResult<usize> {
        debug_assert!(avail > 0);
        let len = buf.len().min(avail);
        self.data.extend_from_slice(&buf[..len]);
        self.wake()?;

        Ok(len)
    }

    #[inline]
    fn available(&self) -> usize {
        self.capacity - self.data.len()
    }
}

impl AsyncRead for Conduit {
    #[inline]
    fn poll_read(
        mut self: Pin<&mut Self>,
        cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<IoResult<()>> {
        if self.data.has_remaining() {
            let count = self.data.remaining().min(buf.remaining());
            if count > 0 {
                if let Err(e) = Conduit::read(&mut self, buf, count) {
                    // This error will never be thrown by a reader
                    unreachable!("JNI exception thrown Rust reader: {:?}", e);
                }
            }
            Poll::Ready(Ok(()))
        } else if self.closed {
            Poll::Ready(Ok(()))
        } else {
            debug_assert!(matches!(self.waiter, Waiter::None));
            self.waiter = Waiter::Reader(Some(cx.waker().clone()));
            Poll::Pending
        }
    }
}

pub struct ByteReader {
    inner: Arc<Mutex<Conduit>>,
}

impl Drop for ByteReader {
    fn drop(&mut self) {
        let guard = &mut *(self.inner.lock());
        if let Err(e) = guard.close_channel() {
            // This error will never be thrown by a reader
            unreachable!("JNI exception thrown Rust reader: {:?}", e);
        }
    }
}

impl AsyncRead for ByteReader {
    fn poll_read(
        self: Pin<&mut Self>,
        cx: &mut Context<'_>,
        buf: &mut ReadBuf<'_>,
    ) -> Poll<IoResult<()>> {
        let inner = &mut *(self.inner.lock());
        Pin::new(inner).poll_read(cx, buf)
    }
}

pub struct ByteWriter {
    inner: Arc<Mutex<Conduit>>,
}

impl ByteWriter {
    #[inline]
    fn try_write(&self, buf: &[u8]) -> JniResult<usize> {
        let ByteWriter { inner } = self;
        let inner = &mut *(inner.lock());

        if inner.closed {
            let env = get_env(&inner.vm)?;
            WRITE_EXCEPTION.throw(&env)?;
            Ok(0)
        } else if buf.is_empty() {
            Ok(0)
        } else {
            let available = inner.available();
            if available == 0 {
                return Ok(0);
            }

            inner.write(buf, available)
        }
    }

    #[inline]
    fn write(&self, mut buf: &[u8], lock: JObject) -> JniResult<()> {
        let ByteWriter { inner } = self;
        let mut guard = inner.lock();
        let mut conduit = &mut *(guard);

        if conduit.closed {
            let env = get_env(&conduit.vm)?;
            WRITE_EXCEPTION.throw(&env)
        } else {
            loop {
                let available = conduit.available();
                if available == 0 {
                    debug_assert!(matches!(conduit.waiter, Waiter::None));

                    conduit.wake()?;

                    let vm = conduit.vm.clone();
                    let env = get_env(&vm)?;
                    let global_ref = env.new_global_ref(lock)?;

                    conduit.waiter = Waiter::Writer(Some(global_ref.clone()));

                    drop(guard);

                    METHOD_OBJECT_WAIT.invoke(&env, &global_ref, &[])?;

                    guard = inner.lock();
                    conduit = &mut *(guard);

                    // We need to loop back again rather than continue here in case this was a
                    // spurious wake up
                    continue;
                }

                let wrote = conduit.write(&buf, available)?;
                buf.advance(wrote);

                conduit.wake()?;
                drop(guard);

                if buf.is_empty() {
                    break Ok(());
                } else {
                    guard = inner.lock();
                    conduit = &mut *(guard);
                }
            }
        }
    }
}

impl Drop for ByteWriter {
    fn drop(&mut self) {
        #[cold]
        #[inline(never)]
        fn fallback_panic(e: impl Debug) -> ! {
            panic!("JNI error: {:?}", e);
        }

        let inner = &mut *(self.inner.lock());

        if let Err(e) = inner.close_channel() {
            match inner.vm.get_env() {
                Ok(env) => abort(&env, e),
                Err(Error::JniCall(JniError::ThreadDetached)) => {
                    match inner.vm.attach_current_thread_as_daemon() {
                        Ok(env) => abort(&env, e),
                        Err(e) => {
                            // not much that can be done here
                            fallback_panic(e);
                        }
                    }
                }
                Err(e) => fallback_panic(e),
            }
        }
    }
}

fn create(env: &JNIEnv, capacity: jint) -> JniResult<ByteWriter> {
    let vm = match env.get_java_vm() {
        Ok(vm) => vm,
        Err(e) => return Err(e),
    };

    let conduit = Arc::new(Mutex::new(Conduit {
        data: BytesMut::new(),
        capacity: capacity as usize,
        vm: Arc::new(vm),
        closed: false,
        waiter: Waiter::None,
    }));

    let mut reader = ByteReader {
        inner: conduit.clone(),
    };

    // todo: remove when finished
    let handle = get_runtime().unwrap();
    handle.spawn(async move {
        let mut buf = BytesMut::new();
        loop {
            if reader.read_buf(&mut buf).await.is_err() {
                break;
            } else {
                buf.clear();
            }
        }
    });

    Ok(ByteWriter { inner: conduit })
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_io_RByteWriter_createNative(
    env: JNIEnv,
    _class: JClass,
    capacity: jint,
) -> *mut ByteWriter {
    match create(&env, capacity) {
        Ok(writer) => Box::into_raw(Box::new(writer)),
        Err(e) => abort(&env, e),
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_io_RByteWriter_tryWrite(
    env: JNIEnv,
    _class: JClass,
    ptr: *mut ByteWriter,
    bytes: jbyteArray,
) -> jint {
    let writer = unsafe { &mut *ptr };

    match env.convert_byte_array(bytes) {
        Ok(buf) => match writer.try_write(buf.as_slice()) {
            Ok(c) => c as jint,
            Err(e) => abort(&env, e),
        },
        Err(e) => abort(&env, e),
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_io_RByteWriter_write(
    env: JNIEnv,
    _class: JClass,
    ptr: *mut ByteWriter,
    bytes: jbyteArray,
    lock: JObject,
) {
    let writer = unsafe { &mut *ptr };

    match env.convert_byte_array(bytes) {
        Ok(buf) => match writer.write(buf.as_slice(), lock) {
            Ok(()) => {}
            Err(e) => abort(&env, e),
        },
        Err(e) => abort(&env, e),
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_io_RByteWriter_releaseNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut ByteWriter,
) {
    unsafe {
        Box::from_raw(ptr);
    }
}
