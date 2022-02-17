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

use crate::io::ffi_byte_channel::{abort, get_env, JniResult};
use crate::java::{InvokeObjectMethod, JavaMethod};
use crate::runtime::get_runtime;
use bytes::BytesMut;
use jni::errors::{Error, JniError};
use jni::objects::{GlobalRef, JClass, JValue};
use jni::sys::{jint, jobject};
use jni::{JNIEnv, JavaVM};
use parking_lot::Mutex;
use rand::{Rng, SeedableRng};
use std::io::{ErrorKind, Result as IoResult};
use tokio::io::{AsyncWrite, AsyncWriteExt};

const METHOD_READ_CALLBACK: JavaMethod = JavaMethod::new("didRead", "([B)V");
const METHOD_CLOSE_CALLBACK: JavaMethod = JavaMethod::new("didClose", "()V");
const JNI_ERROR: &str = "JNI error";

struct Conduit {
    data: BytesMut,
    capacity: usize,
    vm: Arc<JavaVM>,
    closed: bool,
    read_callback: GlobalRef,
    close_callback: GlobalRef,
}

impl Conduit {
    #[inline]
    fn close_channel(&mut self) -> JniResult<()> {
        let Conduit {
            vm,
            closed,
            close_callback,
            ..
        } = self;
        *closed = true;

        let env = get_env(&vm)?;
        METHOD_CLOSE_CALLBACK
            .invoke(&env, close_callback.as_obj(), &[])
            .map(|_| ())
    }

    #[inline]
    fn write(&mut self, buf: &[u8], avail: usize) -> JniResult<usize> {
        debug_assert!(avail > 0);
        let len = buf.len().min(avail);
        self.data.extend_from_slice(&buf[..len]);

        if buf.len() >= len || self.available() == 0 {
            // We only want to invoke the read callback if there isn't more data or the internal
            // buffer is full, as it's an expensive operation
            self.exec_read_callback()?;
        }

        Ok(len)
    }

    #[inline]
    fn exec_read_callback(&mut self) -> JniResult<()> {
        let Conduit {
            data,
            vm,
            read_callback,
            ..
        } = self;

        let env = get_env(&vm)?;
        let buf = env.byte_array_from_slice(data.as_ref())?;

        METHOD_READ_CALLBACK
            .invoke(&env, read_callback.as_obj(), &[JValue::Object(buf.into())])
            .map(|_| ())?;

        data.clear();
        Ok(())
    }

    #[inline]
    fn available(&self) -> usize {
        self.capacity - self.data.len()
    }
}

impl AsyncWrite for Conduit {
    #[inline]
    fn poll_write(
        mut self: Pin<&mut Self>,
        _cx: &mut Context<'_>,
        buf: &[u8],
    ) -> Poll<IoResult<usize>> {
        if self.closed {
            Poll::Ready(Err(ErrorKind::BrokenPipe.into()))
        } else if buf.is_empty() {
            Poll::Ready(Ok(0))
        } else {
            let available = self.capacity - self.data.len();
            // This should never be zero as after each write the Java callback is invoked with the
            // data and the buffer is then cleared.
            debug_assert!(available != 0);

            let len = Conduit::write(&mut self, buf, available).expect(JNI_ERROR);
            Poll::Ready(Ok(len))
        }
    }

    #[inline]
    fn poll_flush(self: Pin<&mut Self>, _: &mut Context<'_>) -> Poll<IoResult<()>> {
        Poll::Ready(Ok(()))
    }

    #[inline]
    fn poll_shutdown(mut self: Pin<&mut Self>, _: &mut Context<'_>) -> Poll<IoResult<()>> {
        self.close_channel().expect(JNI_ERROR);
        Poll::Ready(Ok(()))
    }
}

pub struct ByteReader {
    _inner: Arc<Mutex<Conduit>>,
}

impl Drop for ByteReader {
    fn drop(&mut self) {
        // todo. Should this notify Java?
    }
}

pub struct ByteWriter {
    inner: Arc<Mutex<Conduit>>,
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

impl AsyncWrite for ByteWriter {
    fn poll_write(self: Pin<&mut Self>, cx: &mut Context<'_>, buf: &[u8]) -> Poll<IoResult<usize>> {
        let inner = &mut *(self.inner.lock());
        Pin::new(inner).poll_write(cx, buf)
    }

    fn poll_flush(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<IoResult<()>> {
        let inner = &mut *(self.inner.lock());
        Pin::new(inner).poll_flush(cx)
    }

    fn poll_shutdown(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<IoResult<()>> {
        let inner = &mut *(self.inner.lock());
        Pin::new(inner).poll_shutdown(cx)
    }
}

fn create(
    env: &JNIEnv,
    capacity: jint,
    read_callback: jobject,
    close_callback: jobject,
) -> JniResult<ByteReader> {
    let vm = match env.get_java_vm() {
        Ok(vm) => vm,
        Err(e) => return Err(e),
    };

    let read_callback = env.new_global_ref(read_callback)?;
    let close_callback = env.new_global_ref(close_callback)?;

    let conduit = Arc::new(Mutex::new(Conduit {
        data: BytesMut::new(),
        capacity: capacity as usize,
        vm: Arc::new(vm),
        closed: false,
        read_callback,
        close_callback,
    }));

    let reader = ByteReader {
        _inner: conduit.clone(),
    };

    let mut writer = ByteWriter { inner: conduit };

    // todo: remove when finished
    let handle = get_runtime().unwrap();
    handle.spawn(async move {
        let mut r = rand::rngs::StdRng::from_entropy();

        for i in 10..1000 {
            let mut vec: Vec<u8> = Vec::with_capacity(i);
            for _ in 0..100 {
                vec.push(r.gen());
            }
            writer.write_all(vec.as_slice()).await.unwrap();
        }
    });

    Ok(reader)
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_io_RByteReader_createNative(
    env: JNIEnv,
    _class: JClass,
    capacity: jint,
    read_callback: jobject,
    close_callback: jobject,
) -> *mut ByteReader {
    match create(&env, capacity, read_callback, close_callback) {
        Ok(reader) => Box::into_raw(Box::new(reader)),
        Err(e) => abort(&env, e),
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_io_RByteReader_releaseNative(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut ByteWriter,
) {
    unsafe {
        Box::from_raw(ptr);
    }
}
