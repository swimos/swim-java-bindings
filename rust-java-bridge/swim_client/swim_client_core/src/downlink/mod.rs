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

use bytes::{Buf, BytesMut};
use std::mem::size_of;
use std::num::NonZeroUsize;
use std::time::Duration;
use swim_api::downlink::DownlinkConfig;
use swim_runtime::downlink::{DownlinkOptions, DownlinkRuntimeConfig};

const CONFIG_LEN: usize = size_of::<u64>() * 5 + size_of::<u8>() * 4;

mod decoder;
pub mod map;
pub mod value;
mod vtable;

/// A wrapper around a downlink runtime configuration, downlink configuration and downlink options
/// that can be parsed from a Java-provided byte array of the following format:
///
/// u64 duration -> Duration
/// u64 attachment_queue_size -> NonZeroUSize
/// u8 abort_on_bad_frames -> bool
/// u64 remote_buffer_size -> NonZeroUSize
/// u64 downlink_buffer_size -> NonZeroUSize
/// u8 events_when_not_synced -> bool
/// u8 terminate_on_unlinked -> bool
/// u64 buffer_size -> NonZeroUSize
/// u8 downlink options -> bool
// todo: this can be removed once the builder module has been merged
#[derive(Debug, PartialEq, Eq)]
pub struct DownlinkConfigurations {
    pub runtime: DownlinkRuntimeConfig,
    pub downlink: DownlinkConfig,
    pub options: DownlinkOptions,
}

impl Default for DownlinkConfigurations {
    fn default() -> Self {
        DownlinkConfigurations {
            runtime: Default::default(),
            downlink: Default::default(),
            options: DownlinkOptions::DEFAULT,
        }
    }
}

impl DownlinkConfigurations {
    pub fn try_from_bytes(buf: &mut BytesMut) -> Result<DownlinkConfigurations, String> {
        if buf.len() != CONFIG_LEN {
            return Err("Invalid buffer length".to_string());
        };

        Ok(DownlinkConfigurations {
            runtime: read_runtime_config(buf)?,
            downlink: read_downlink_config(buf)?,
            options: read_options_config(buf)?,
        })
    }
}

fn read_runtime_config(buf: &mut BytesMut) -> Result<DownlinkRuntimeConfig, String> {
    Ok(DownlinkRuntimeConfig {
        empty_timeout: Duration::from_secs(buf.get_u64()),
        attachment_queue_size: parse_non_zero_usize(buf)?,
        abort_on_bad_frames: parse_bool(buf)?,
        remote_buffer_size: parse_non_zero_usize(buf)?,
        downlink_buffer_size: parse_non_zero_usize(buf)?,
    })
}

fn read_downlink_config(buf: &mut BytesMut) -> Result<DownlinkConfig, String> {
    Ok(DownlinkConfig {
        events_when_not_synced: parse_bool(buf)?,
        terminate_on_unlinked: parse_bool(buf)?,
        buffer_size: parse_non_zero_usize(buf)?,
    })
}

fn read_options_config(buf: &mut BytesMut) -> Result<DownlinkOptions, String> {
    let opts = buf.get_u8();
    DownlinkOptions::from_bits(opts).ok_or(format!("Invalid downlink options bits: {:b}", opts))
}

fn parse_non_zero_usize(buf: &mut BytesMut) -> Result<NonZeroUsize, String> {
    let int = buf.get_u64();
    NonZeroUsize::new(int as usize).ok_or(format!("Invalid non-zero integer: {}", int))
}

fn parse_bool(buf: &mut BytesMut) -> Result<bool, String> {
    let b = buf.get_u8();
    if b > 1 {
        Err(format!("Invalid boolean: {}", b))
    } else {
        Ok(b == 1)
    }
}

#[cfg(test)]
mod test {
    use crate::downlink::DownlinkConfigurations;
    use bytes::{BufMut, BytesMut};
    use std::time::Duration;
    use swim_api::downlink::DownlinkConfig;
    use swim_runtime::downlink::{DownlinkOptions, DownlinkRuntimeConfig};
    use swim_utilities::non_zero_usize;

    #[test]
    fn parse_valid_config() {
        let mut buf = BytesMut::new();
        buf.put_u64(5); // duration
        buf.put_u64(32); // attachment_queue_size
        buf.put_u8(0); // abort_on_bad_frames
        buf.put_u64(16); // remote_buffer_size
        buf.put_u64(32); // downlink_buffer_size
        buf.put_u8(0); // events_when_not_synced
        buf.put_u8(1); // terminate_on_unlinked
        buf.put_u64(64); // downlink buffer_size
        buf.put_u8(0b11); // downlink options

        let config =
            DownlinkConfigurations::try_from_bytes(&mut buf).expect("Expected a valid config");
        assert_eq!(
            config,
            DownlinkConfigurations {
                runtime: DownlinkRuntimeConfig {
                    empty_timeout: Duration::from_secs(5),
                    attachment_queue_size: non_zero_usize!(32),
                    abort_on_bad_frames: false,
                    remote_buffer_size: non_zero_usize!(16),
                    downlink_buffer_size: non_zero_usize!(32),
                },
                downlink: DownlinkConfig {
                    events_when_not_synced: false,
                    terminate_on_unlinked: true,
                    buffer_size: non_zero_usize!(64),
                },
                options: DownlinkOptions::SYNC | DownlinkOptions::KEEP_LINKED,
            }
        );
    }

    #[test]
    fn parse_invalid_non_zero_usize() {
        let mut buf = BytesMut::new();
        buf.put_u64(0);
        buf.put_u64(0);
        buf.put_u8(0);
        buf.put_u64(0);
        buf.put_u64(0);
        buf.put_u8(0);
        buf.put_u8(0);
        buf.put_u64(0);
        buf.put_u8(0);

        assert_eq!(
            DownlinkConfigurations::try_from_bytes(&mut buf).unwrap_err(),
            "Invalid non-zero integer: 0"
        );
    }

    #[test]
    fn parse_invalid_bool() {
        let mut buf = BytesMut::new();
        buf.put_u64(5);
        buf.put_u64(32);
        buf.put_u8(0);
        buf.put_u64(16);
        buf.put_u64(32);
        buf.put_u8(0);
        buf.put_u8(1);
        buf.put_u64(64);
        buf.put_u8(0b111);

        assert_eq!(
            DownlinkConfigurations::try_from_bytes(&mut buf).unwrap_err(),
            "Invalid downlink options bits: 111"
        );
    }

    #[test]
    fn parse_invalid_length() {
        let mut buf = BytesMut::new();
        assert_eq!(
            DownlinkConfigurations::try_from_bytes(&mut buf).unwrap_err(),
            "Invalid buffer length"
        );

        let mut buf = BytesMut::with_capacity(1000);
        unsafe {
            buf.set_len(123);
        }
        assert_eq!(
            DownlinkConfigurations::try_from_bytes(&mut buf).unwrap_err(),
            "Invalid buffer length"
        );
    }
}
