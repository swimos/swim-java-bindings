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

///! Unit tests invoked by swim-bridge unit tests.
use std::future::Future;
use std::io::ErrorKind;
use std::io::ErrorKind::BrokenPipe;

use bytes::BytesMut;
use jni::objects::{JByteBuffer, JClass};
use jni::sys::{jbyteArray, jint, jobject};
use jni::JNIEnv;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::runtime::{Builder, Runtime};

use jvm_sys::channel::reader::ByteReader;
use jvm_sys::channel::writer::ByteWriter;
use jvm_sys::vm::method::InvokeObjectMethod;
use jvm_sys::vm::method::JavaMethod;
use jvm_sys::vm::utils::get_env;
use sys_util::{jvm_tryf, npch};

/// Creates a new multi-threaded Tokio runtime and spawns 'fut' on to it. A monitor task is spawned
/// that waits for 'fut' to complete and then notifies 'barrier' that the task has completed; this
/// task must be run as its own Tokio task in case 'fut' panics as it is not possible to catch an
/// unwind due to uses of &mut T by readers and writers.
fn run_test<F>(env: JNIEnv, barrier: jobject, fut: F) -> *mut Runtime
where
    F: Future + Send + 'static,
    F::Output: Send + 'static,
{
    npch!(env, barrier);

    let vm = env.get_java_vm().unwrap();
    let global_ref = env.new_global_ref(barrier).unwrap();

    let runtime = Builder::new_multi_thread()
        .build()
        .expect("Failed to build runtime");

    let join_handle = runtime.spawn(fut);
    runtime.spawn(async move {
        println!("Rust: started watch");
        let r = join_handle.await;
        println!("Rust: test complete");
        let env = get_env(&vm).unwrap();
        let _guard = env.lock_obj(&global_ref).expect("Failed to enter monitor");
        println!("Rust: notifying");
        jvm_tryf!(env, JavaMethod::NOTIFY.invoke(&env, &global_ref, &[]));
        println!("Rust: notified");
        if r.is_err() {
            env.fatal_error("Test panicked");
        }
    });

    Box::leak(Box::new(runtime))
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_channel_FfiChannelTest_readerTask(
    env: JNIEnv,
    _class: JClass,
    bb: JByteBuffer,
    monitor: jobject,
    buf: jbyteArray,
    barrier: jobject,
) -> *mut Runtime {
    npch!(env, bb, monitor, buf);

    let expected = env.convert_byte_array(buf).unwrap();
    let mut reader = ByteReader::new(env, bb, monitor);

    run_test(env, barrier, async move {
        let mut buf = BytesMut::new();
        buf.reserve(buf.len() * 2);

        println!("Rust: starting read loop");

        loop {
            match reader.read_buf(&mut buf).await {
                Ok(_) => {
                    println!("Rust: read");
                }
                Err(e) if e.kind() == ErrorKind::BrokenPipe => {
                    println!("Rust: broken pipe");
                    let slice = buf.as_ref();
                    let expected_len = expected.len();
                    let actual_len = slice.len();

                    if expected.len() == slice.len() {
                        assert_eq!(&slice[..expected.len()], expected.as_slice());
                        assert!(!slice[expected.len()..].iter().any(|e| *e == 0));
                        println!("Rust: eq");
                        break;
                    } else {
                        panic!(
                            "Expected ({}) and actual ({}) buffer lengths differ",
                            expected_len, actual_len
                        );
                    }
                }
                Err(e) => panic!("{:?}", e),
            }
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_channel_FfiChannelTest_writerTask(
    env: JNIEnv,
    _class: JClass,
    bb: JByteBuffer,
    monitor: jobject,
    buf: jbyteArray,
    chunk_size: jint,
    barrier: jobject,
) -> *mut Runtime {
    npch!(env, bb, monitor, buf);

    let mut to_write = env.convert_byte_array(buf).unwrap();
    let mut writer = ByteWriter::new(env, bb, monitor);

    run_test(env, barrier, async move {
        for chunk in to_write.chunks_mut(chunk_size as usize) {
            if let Err(e) = writer.write_all(chunk).await {
                panic!("Unexpected write failure: {:?}", e);
            }
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_channel_FfiChannelTest_writerClosedTask(
    env: JNIEnv,
    _class: JClass,
    bb: JByteBuffer,
    monitor: jobject,
    barrier: jobject,
) -> *mut Runtime {
    npch!(env, bb, monitor);

    let mut writer = ByteWriter::new(env, bb, monitor);

    run_test(env, barrier, async move {
        match writer.write_all(&[1, 2, 3, 4, 5]).await {
            Ok(_) => {
                panic!("Unexpected write")
            }
            Err(e) if e.kind() == BrokenPipe => {}
            Err(e) => panic!("Unexpected error: {:?}", e),
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_channel_FfiChannelTest_dropWriterTask(
    env: JNIEnv,
    _class: JClass,
    bb: JByteBuffer,
    monitor: jobject,
    barrier: jobject,
) -> *mut Runtime {
    npch!(env, bb, monitor);

    let writer = ByteWriter::new(env, bb, monitor);
    run_test(env, barrier, async move {
        drop(writer);
    })
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_channel_FfiChannelTest_dropReaderTask(
    env: JNIEnv,
    _class: JClass,
    bb: JByteBuffer,
    monitor: jobject,
    barrier: jobject,
) -> *mut Runtime {
    npch!(env, bb, monitor);

    let reader = ByteReader::new(env, bb, monitor);
    run_test(env, barrier, async move { drop(reader) })
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_bridge_channel_FfiChannelTest_dropRuntime(
    _env: JNIEnv,
    _class: JClass,
    runtime: *mut Runtime,
) {
    drop(Box::new(runtime));
}
