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

use std::sync::Arc;

use bytes::BytesMut;
use futures_util::future::BoxFuture;
use futures_util::StreamExt;
use jni::errors::Error;
use jni::sys::jobject;
use jni::JavaVM;
use swim_api::downlink::{Downlink, DownlinkConfig, DownlinkKind};
use swim_api::error::{DownlinkTaskError, FrameIoError};
use swim_api::protocol::downlink::{DownlinkNotification, ValueNotificationDecoder};
use swim_model::address::Address;
use swim_model::{Text, Value};
use swim_recon::printer::print_recon_compact;
use swim_utilities::io::byte_channel::{ByteReader, ByteWriter};
use tokio_util::codec::FramedRead;

use jvm_sys::vm::utils::VmExt;
use jvm_sys::vm::SpannedError;

use crate::downlink::value::lifecycle::ValueDownlinkLifecycle;
use crate::downlink::{ErrorHandlingConfig, FfiFailureHandler};

mod lifecycle;
pub mod vtable;

pub type SharedVm = Arc<JavaVM>;

pub struct FfiValueDownlink {
    vm: SharedVm,
    lifecycle: ValueDownlinkLifecycle,
    handler: Box<dyn FfiFailureHandler>,
}

impl FfiValueDownlink {
    pub fn create(
        vm: SharedVm,
        on_event: jobject,
        on_linked: jobject,
        on_set: jobject,
        on_synced: jobject,
        on_unlinked: jobject,
        error_mode: ErrorHandlingConfig,
    ) -> Result<FfiValueDownlink, Error> {
        let env = vm.get_env()?;
        let lifecycle = ValueDownlinkLifecycle::from_parts(
            &env,
            on_event,
            on_linked,
            on_set,
            on_synced,
            on_unlinked,
        )?;
        Ok(FfiValueDownlink {
            vm,
            lifecycle,
            handler: error_mode.as_handler(),
        })
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
            let FfiValueDownlink {
                vm,
                lifecycle,
                handler,
            } = self;
            match run_ffi_value_downlink(vm, lifecycle, path, config, input, output).await {
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

enum LinkState {
    Unlinked,
    Linked(Option<Vec<u8>>),
    Synced,
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

async fn run_ffi_value_downlink(
    vm: SharedVm,
    mut lifecycle: ValueDownlinkLifecycle,
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

    let mut state = LinkState::Unlinked;
    let mut framed_read = FramedRead::new(input, ValueNotificationDecoder::<Value>::default());
    let mut ffi_buffer = BytesMut::new();

    while let Some(result) = framed_read.next().await {
        let env = vm.expect_env();
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
                    return Err(DownlinkTaskError::SyncedWithNoValue.into());
                }
            },
            DownlinkNotification::Event { body } => {
                let mut data = format!("{}", print_recon_compact(&body)).into_bytes();
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
