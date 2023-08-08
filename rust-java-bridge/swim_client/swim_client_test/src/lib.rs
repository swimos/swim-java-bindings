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

use crate::value::Notification;
use bytes::BytesMut;
use futures_util::future::try_join3;
use futures_util::SinkExt;
use jni::objects::JString;
use jni::sys::jbyteArray;
use jni::sys::jobject;
use jni::JavaVM;
use jvm_sys::jni_try;
use jvm_sys::vm::utils::VmExt;
use jvm_sys_tests::run_test;
use std::io::ErrorKind;
use std::sync::Arc;
use swim_api::downlink::{Downlink, DownlinkConfig};
use swim_api::protocol::downlink::{DownlinkNotification, DownlinkNotificationEncoder};
use swim_client_core::downlink::DownlinkConfigurations;
use swim_client_core::{client_fn, SwimClient};
use swim_model::address::Address;
use swim_model::Blob;
use swim_recon::parser::{parse_recognize, Span};
use swim_recon::printer::print_recon_compact;
use swim_utilities::io::byte_channel::byte_channel;
use swim_utilities::non_zero_usize;
use tokio::io::AsyncReadExt;
use tokio::runtime::Runtime;
use tokio_util::codec::FramedWrite;

mod map;
mod value;

client_fn! {
    downlink_FfiTest_dropSwimClient(
        _env,
        _class,
        ptr: *mut SwimClient,
    ) {
        println!("Dropping swim client");
        unsafe {
            drop(Box::from_raw(ptr));
        }
    }
}

client_fn! {
    downlink_FfiTest_dropRuntime(
        _env,
        _class,
        ptr: *mut Runtime,
    ) {
        unsafe {
            drop(Box::from_raw(ptr));
        }
    }
}

fn lifecycle_test<'a, D>(
    downlink: D,
    vm: Arc<JavaVM>,
    lock: jobject,
    input: JString<'a>,
    host: JString<'a>,
    node: JString<'a>,
    lane: JString<'a>,
    events_when_not_synced: bool,
) -> *mut Runtime
where
    D: Downlink + Send + 'static,
{
    let env = vm.env_or_abort();
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
            events_when_not_synced,
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

client_fn! {
    downlink_ConfigTest_parsesConfig(
        env,
        _class,
        config: jbyteArray,
    ) {
        let mut config_bytes = jni_try! {
            env,
            "Failed to parse configuration array",
            env.convert_byte_array(config).map(BytesMut::from_iter)
        };

        let _r = DownlinkConfigurations::try_from_bytes(&mut config_bytes, &env);
    }
}
