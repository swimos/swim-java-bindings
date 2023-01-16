// Copyright 2015-2022 Swim Inc.
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

use std::io::ErrorKind;
use std::sync::Arc;

use bytes::BytesMut;
use futures_util::future::try_join3;
use futures_util::SinkExt;
use jni::objects::{JClass, JString};
use jni::sys::jobject;
use jni::JNIEnv;
use jvm_sys::vm::set_panic_hook;
use swim_api::downlink::{Downlink, DownlinkConfig};
use swim_api::protocol::downlink::{DownlinkNotification, DownlinkNotificationEncoder};
use swim_form::Form;
use swim_model::address::Address;
use swim_model::{Blob, Text, Value};
use swim_recon::parser::{parse_recognize, Span};
use swim_recon::printer::print_recon_compact;
use swim_utilities::algebra::non_zero_usize;
use swim_utilities::io::byte_channel::byte_channel;
use tokio::io::AsyncReadExt;
use tokio::runtime::Runtime;
use tokio_util::codec::FramedWrite;

use jvm_sys_tests::run_test;
use swim_client_core::downlink::value::FfiValueDownlink;
use swim_client_core::downlink::ErrorHandlingConfig;

#[derive(Clone, Debug, PartialEq, Form)]
#[form_root(::swim_form)]
pub enum Notification {
    #[form(tag = "linked")]
    Linked {
        #[form(header)]
        node: Text,
        #[form(header)]
        lane: Text,
    },
    #[form(tag = "synced")]
    Synced {
        node: Text,
        lane: Text,
        #[form(body)]
        body: Option<Blob>,
    },
    #[form(tag = "unlinked")]
    Unlinked {
        #[form(header)]
        node: Text,
        #[form(header)]
        lane: Text,
    },
    #[form(tag = "event")]
    Event {
        node: Text,
        lane: Text,
        #[form(body)]
        body: Option<Value>,
    },
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_downlink_value_ValueDownlinkTest_nativeTest(
    env: JNIEnv,
    _class: JClass,
    lock: jobject,
    input: JString,
    host: JString,
    node: JString,
    lane: JString,
    on_event: jobject,
    on_linked: jobject,
    on_set: jobject,
    on_synced: jobject,
    on_unlinked: jobject,
) -> *mut Runtime {
    set_panic_hook();
    let vm = Arc::new(env.get_java_vm().unwrap());
    let downlink = FfiValueDownlink::create(
        vm.clone(),
        on_event,
        on_linked,
        on_set,
        on_synced,
        on_unlinked,
        ErrorHandlingConfig::Abort,
    )
    .expect("Failed to build downlink");

    let host = env.get_string(host).unwrap();
    let node = env.get_string(node).unwrap();
    let lane = env.get_string(lane).unwrap();

    let (input_tx, input_rx) = byte_channel(non_zero_usize!(128));
    let (output_tx, mut output_rx) = byte_channel(non_zero_usize!(128));
    let input = env.get_string(input).unwrap().to_str().unwrap().to_string();

    let write_task = async move {
        let mut framed = FramedWrite::new(input_tx, DownlinkNotificationEncoder);
        for notif in input.lines() {
            match parse_recognize::<Notification>(Span::new(notif), false).unwrap() {
                Notification::Linked { .. } => framed
                    .send(DownlinkNotification::<Blob>::Linked)
                    .await
                    .expect("Failed to encode message"),
                Notification::Synced { .. } => framed
                    .send(DownlinkNotification::<Blob>::Synced)
                    .await
                    .expect("Failed to encode message"),
                Notification::Unlinked { .. } => framed
                    .send(DownlinkNotification::<Blob>::Unlinked)
                    .await
                    .expect("Failed to encode message"),
                Notification::Event { body, .. } => framed
                    .send(DownlinkNotification::Event {
                        body: format!("{}", print_recon_compact(&body)).into_bytes(),
                    })
                    .await
                    .expect("Failed to encode message"),
            }
        }

        Ok(())
    };

    let read_task = async move {
        let mut buf = BytesMut::new();
        match output_rx.read(&mut buf).await {
            Ok(0) => {}
            Ok(_) => {
                panic!("Unexpected downlink value read: {:?}", buf.as_ref());
            }
            Err(e) => {
                if e.kind() != ErrorKind::BrokenPipe {
                    panic!("Downlink read channel error: {:?}", e);
                }
            }
        }

        Ok(())
    };

    let downlink_task = downlink.run(
        Address::new(
            Some(host.to_str().unwrap().into()),
            node.to_str().unwrap().into(),
            lane.to_str().unwrap().into(),
        ),
        DownlinkConfig {
            events_when_not_synced: true,
            terminate_on_unlinked: false,
            buffer_size: non_zero_usize!(1024),
        },
        input_rx,
        output_tx,
    );

    let task = async move {
        try_join3(write_task, read_task, downlink_task)
            .await
            .expect("Downlink task failure");
    };

    run_test(env, lock, task)
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_downlink_value_ValueDownlinkTest_dropRuntime(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Runtime,
) {
    unsafe {
        drop(Box::from_raw(ptr));
    }
}

#[no_mangle]
pub extern "system" fn Java_ai_swim_client_downlink_value_ValueDownlinkTest_driveDownlink(
    _env: JNIEnv,
    _class: JClass,
    ptr: *mut Runtime,
) {
}
