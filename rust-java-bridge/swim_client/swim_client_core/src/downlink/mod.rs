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

use swim_api::error::DownlinkTaskError;

use jvm_sys::vm::SpannedError;

pub mod value;

#[derive(Copy, Clone, Debug)]
pub enum ErrorHandlingConfig {
    Ignore,
    CloseDownlink,
    Abort,
}

impl ErrorHandlingConfig {
    pub fn as_handler(&self) -> Box<dyn FfiFailureHandler> {
        match self {
            ErrorHandlingConfig::CloseDownlink => Box::new(FailingHandler),
            ErrorHandlingConfig::Abort => Box::new(AbortingHandler),
            ErrorHandlingConfig::Ignore => Box::new(SinkingHandler),
        }
    }
}

pub trait FfiFailureHandler: Send + Sync {
    fn on_failure(&self, err: SpannedError) -> Result<(), DownlinkTaskError>;
}

impl FfiFailureHandler for Box<dyn FfiFailureHandler> {
    fn on_failure(&self, err: SpannedError) -> Result<(), DownlinkTaskError> {
        (**self).on_failure(err)
    }
}

struct AbortingHandler;
impl FfiFailureHandler for AbortingHandler {
    fn on_failure(&self, err: SpannedError) -> Result<(), DownlinkTaskError> {
        #[cold]
        #[inline(never)]
        fn abort(err: SpannedError) -> ! {
            err.panic()
        }

        abort(err);
    }
}

struct FailingHandler;
impl FfiFailureHandler for FailingHandler {
    fn on_failure(&self, err: SpannedError) -> Result<(), DownlinkTaskError> {
        let SpannedError { cause, .. } = err;
        Err(DownlinkTaskError::Custom(cause))
    }
}

struct SinkingHandler;
impl FfiFailureHandler for SinkingHandler {
    fn on_failure(&self, _err: SpannedError) -> Result<(), DownlinkTaskError> {
        Ok(())
    }
}
