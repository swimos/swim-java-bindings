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

use bytes::BytesMut;
use futures_util::future::BoxFuture;
use futures_util::StreamExt;
use jni::sys::jobject;
use swim_api::downlink::{Downlink, DownlinkConfig, DownlinkKind};
use swim_api::error::DownlinkTaskError;
use swim_api::protocol::downlink::{DownlinkNotification};
use swim_model::{Text};
use swim_model::address::Address;
use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
use tokio_util::codec::FramedRead;

use jvm_sys::env::JavaEnv;

use crate::downlink::decoder::ValueDlNotDecoder;
use crate::downlink::value::lifecycle::ValueDownlinkLifecycle;

mod lifecycle;

pub struct FfiValueDownlink {
    env: JavaEnv,
    lifecycle: ValueDownlinkLifecycle,
}

impl FfiValueDownlink {
    pub fn create(
        env: JavaEnv,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
    ) -> FfiValueDownlink {
        let lifecycle = ValueDownlinkLifecycle::from_parts(
            &env,
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        );
        FfiValueDownlink { env, lifecycle }
    }
}

impl Downlink for FfiValueDownlink {
    fn kind(&self) -> DownlinkKind {
        DownlinkKind::Value
    }

    fn run(
        self,
        path: Address<Text>,
        config: DownlinkConfig,
        input: ByteReader,
        output: ByteWriter,
    ) -> BoxFuture<'static, Result<(), DownlinkTaskError>> {
        Box::pin(async move {
            let FfiValueDownlink { env, lifecycle } = self;
            run_ffi_value_downlink(env, lifecycle, path, config, input, output).await
        })
    }

    fn run_boxed(
        self: Box<Self>,
        path: Address<Text>,
        config: DownlinkConfig,
        input: ByteReader,
        output: ByteWriter,
    ) -> BoxFuture<'static, Result<(), DownlinkTaskError>> {
        (*self).run(path, config, input, output)
    }
}

enum LinkState {
    Unlinked,
    Linked(Option<Vec<u8>>),
    Synced,
}

async fn run_ffi_value_downlink(
    env: JavaEnv,
    mut lifecycle: ValueDownlinkLifecycle,
    _path: Address<Text>,
    config: DownlinkConfig,
    input: ByteReader,
    _output: ByteWriter,
) -> Result<(), DownlinkTaskError> {
    let DownlinkConfig {
        events_when_not_synced,
        terminate_on_unlinked,
        ..
    } = config;

    let mut state = LinkState::Unlinked;
    let mut framed_read = FramedRead::new(input, ValueDlNotDecoder::default());
    let mut ffi_buffer = BytesMut::new();

    while let Some(result) = framed_read.next().await {
        match result? {
            DownlinkNotification::Linked => {
                if matches!(&state, LinkState::Unlinked) {
                    lifecycle.on_linked(&env)?;
                    state = LinkState::Linked(None);
                }
            }
            DownlinkNotification::Synced => match state {
                LinkState::Linked(Some(mut data)) => {
                    lifecycle.on_synced(&env, &mut data, &mut ffi_buffer)?;
                    state = LinkState::Synced;
                }
                _ => {
                    return Err(DownlinkTaskError::SyncedWithNoValue);
                }
            },
            DownlinkNotification::Event { body } => {
                let mut data = body.to_vec();
                match &mut state {
                    LinkState::Linked(value) => {
                        if events_when_not_synced {
                            lifecycle.on_event(&env, &mut data, &mut ffi_buffer)?;
                            lifecycle.on_set(&env, &mut data, &mut ffi_buffer)?;
                        }
                        *value = Some(data);
                    }
                    LinkState::Synced => {
                        lifecycle.on_event(&env, &mut data, &mut ffi_buffer)?;
                        lifecycle.on_set(&env, &mut data, &mut ffi_buffer)?;
                    }
                    LinkState::Unlinked => {}
                }
            }
            DownlinkNotification::Unlinked => {
                lifecycle.on_unlinked(&env)?;
                if terminate_on_unlinked {
                    break;
                } else {
                    state = LinkState::Unlinked;
                }
            }
        }
    }

    Ok(())
}
