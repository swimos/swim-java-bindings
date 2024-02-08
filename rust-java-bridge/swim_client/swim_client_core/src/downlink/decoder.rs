// Copyright 2015-2024 Swim Inc.
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

use bytes::{Bytes, BytesMut};
use swim_api::error::{FrameIoError, InvalidFrame};
use swim_api::protocol::downlink::{DownlinkNotification, DownlinkNotificationDecoder};
use swim_api::protocol::map::{extract_header, MapMessage};
use swim_recon::parser::MessageExtractError;
use tokio_util::codec::Decoder;

pub struct MapDlNotDecoder {
    delegate: DownlinkNotificationDecoder<MapMessage<Bytes, Bytes>, MapMessageDecoder>,
}

impl Default for MapDlNotDecoder {
    fn default() -> Self {
        MapDlNotDecoder {
            delegate: DownlinkNotificationDecoder::new(MapMessageDecoder),
        }
    }
}

impl Decoder for MapDlNotDecoder {
    type Item = DownlinkNotification<MapMessage<Bytes, Bytes>>;
    type Error = FrameIoError;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        self.delegate.decode(src)
    }
}

struct MapMessageDecoder;

impl Decoder for MapMessageDecoder {
    type Item = MapMessage<Bytes, Bytes>;
    type Error = FrameIoError;

    fn decode(&mut self, _src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        Ok(None)
    }

    fn decode_eof(&mut self, buf: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        let src = buf.clone().freeze();
        buf.clear();
        Ok(Some(extract_header(&src).map_err(|e| match e {
            MessageExtractError::BadUtf8(_) => FrameIoError::Io(ErrorKind::InvalidData.into()),
            MessageExtractError::ParseError(e) => {
                FrameIoError::BadFrame(InvalidFrame::InvalidHeader { problem: e.into() })
            }
        })?))
    }
}

pub struct ValueDlNotDecoder {
    delegate: DownlinkNotificationDecoder<Bytes, ValueMessageDecoder>,
}

impl Default for ValueDlNotDecoder {
    fn default() -> Self {
        ValueDlNotDecoder {
            delegate: DownlinkNotificationDecoder::new(ValueMessageDecoder),
        }
    }
}

impl Decoder for ValueDlNotDecoder {
    type Item = DownlinkNotification<Bytes>;
    type Error = FrameIoError;

    fn decode(&mut self, src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        self.delegate.decode(src)
    }
}

pub struct ValueMessageDecoder;

impl Decoder for ValueMessageDecoder {
    type Item = Bytes;
    type Error = FrameIoError;

    fn decode(&mut self, _src: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        Ok(None)
    }

    fn decode_eof(&mut self, buf: &mut BytesMut) -> Result<Option<Self::Item>, Self::Error> {
        let src = buf.clone().freeze();
        buf.clear();
        Ok(Some(src))
    }
}
