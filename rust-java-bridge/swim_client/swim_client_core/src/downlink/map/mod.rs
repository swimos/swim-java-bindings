mod lifecycle;

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

use bytes::Bytes;
use futures_util::future::BoxFuture;
use futures_util::StreamExt;
use jni::errors::Error;
use jni::sys::{jint, jobject};
use jni::JNIEnv;
use swim_api::downlink::{Downlink, DownlinkConfig, DownlinkKind};
use swim_api::error::{DownlinkTaskError, FrameIoError};
use swim_api::protocol::downlink::DownlinkNotification;
use swim_api::protocol::map::MapMessage;
use swim_model::address::Address;
use swim_model::Text;
use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
use tokio_util::codec::FramedRead;

use jvm_sys::vm::utils::VmExt;
use jvm_sys::vm::SpannedError;

use crate::downlink::decoder::MapDlNotDecoder;
pub use crate::downlink::map::lifecycle::MapDownlinkLifecycle;
use crate::downlink::{ErrorHandlingConfig, FfiFailureHandler};
use crate::SharedVm;

pub struct FfiMapDownlink {
    vm: SharedVm,
    lifecycle: MapDownlinkLifecycle,
    handler: Box<dyn FfiFailureHandler>,
}

impl FfiMapDownlink {
    pub fn create(
        vm: SharedVm,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
        error_mode: ErrorHandlingConfig,
    ) -> Result<FfiMapDownlink, Error> {
        let env = vm.expect_env();
        let lifecycle = MapDownlinkLifecycle::from_parts(
            &env,
            on_linked,
            on_synced,
            on_update,
            on_remove,
            on_clear,
            on_unlinked,
            take,
            drop,
        )?;
        Ok(FfiMapDownlink {
            vm,
            lifecycle,
            handler: error_mode.as_handler(),
        })
    }
}

impl Downlink for FfiMapDownlink {
    fn kind(&self) -> DownlinkKind {
        DownlinkKind::Map
    }

    fn run(
        self,
        path: Address<Text>,
        config: DownlinkConfig,
        input: ByteReader,
        output: ByteWriter,
    ) -> BoxFuture<'static, Result<(), DownlinkTaskError>> {
        Box::pin(async move {
            let FfiMapDownlink {
                vm,
                lifecycle,
                handler,
            } = self;
            match run_ffi_map_downlink(vm, lifecycle, path, config, input, output).await {
                Ok(()) => Ok(()),
                Err(RuntimeError::Downlink(err)) => Err(err),
                Err(RuntimeError::Ffi(err)) => handler.on_failure(err),
            }
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

enum RuntimeError {
    Downlink(DownlinkTaskError),
    Ffi(SpannedError),
}

impl From<FrameIoError> for RuntimeError {
    fn from(value: FrameIoError) -> Self {
        RuntimeError::Downlink(DownlinkTaskError::BadFrame(value))
    }
}

impl From<DownlinkTaskError> for RuntimeError {
    fn from(value: DownlinkTaskError) -> Self {
        RuntimeError::Downlink(value)
    }
}

impl From<SpannedError> for RuntimeError {
    fn from(value: SpannedError) -> Self {
        RuntimeError::Ffi(value)
    }
}

/// The current state of the downlink.
enum State {
    Unlinked,
    Linked,
    Synced,
}

async fn run_ffi_map_downlink(
    vm: SharedVm,
    mut lifecycle: MapDownlinkLifecycle,
    _path: Address<Text>,
    config: DownlinkConfig,
    input: ByteReader,
    _output: ByteWriter,
) -> Result<(), RuntimeError> {
    let DownlinkConfig {
        events_when_not_synced,
        terminate_on_unlinked,
        ..
    } = config;

    let mut state = State::Unlinked;
    let mut framed_read = FramedRead::new(input, MapDlNotDecoder::default());

    while let Some(result) = framed_read.next().await {
        let env = vm.expect_env();

        match result? {
            DownlinkNotification::Linked => {
                if matches!(&state, State::Unlinked) {
                    lifecycle.on_linked(&env)?;
                    state = State::Linked;
                }
            }
            DownlinkNotification::Synced => match state {
                State::Linked => {
                    lifecycle.on_synced(&env)?;
                    state = State::Synced;
                }
                _ => {}
            },
            DownlinkNotification::Event { body } => match &mut state {
                State::Unlinked => {}
                State::Linked => on_event(&env, &mut lifecycle, body, events_when_not_synced)?,
                State::Synced => on_event(&env, &mut lifecycle, body, true)?,
            },
            DownlinkNotification::Unlinked => {
                lifecycle.on_unlinked(&env)?;
                if terminate_on_unlinked {
                    break;
                } else {
                    state = State::Unlinked;
                }
            }
        }
    }

    Ok(())
}

fn on_event(
    env: &JNIEnv,
    lifecycle: &mut MapDownlinkLifecycle,
    event: MapMessage<Bytes, Bytes>,
    dispatch: bool,
) -> Result<(), SpannedError> {
    match event {
        MapMessage::Update { key, value } => {
            lifecycle.on_update(env, key.to_vec(), value.to_vec(), dispatch)
        }
        MapMessage::Remove { key } => lifecycle.on_remove(env, key.to_vec(), dispatch),
        MapMessage::Clear => lifecycle.on_clear(env, dispatch),
        MapMessage::Take(cnt) => lifecycle.take(env, cnt as jint, dispatch),
        MapMessage::Drop(cnt) => lifecycle.drop(env, cnt as jint, dispatch),
    }
}
