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

use crate::{ByteCodec, FromBytesError};
use bytes::{Buf, BufMut, BytesMut};
use rmp::Marker;
use std::collections::HashMap;
use std::fmt::Debug;
use std::num::NonZeroUsize;
use std::time::Duration;
use std::usize;

fn round_trip_ok<B>(value: B)
where
    B: PartialEq + Debug + ByteCodec,
{
    let buf = BytesMut::new();
    let mut writer = buf.writer();
    value.to_bytes(&mut writer).expect("Failed to write bytes");

    let mut reader = writer.into_inner().reader();

    match B::try_from_reader(&mut reader) {
        Ok(actual) => {
            let buf = reader.into_inner();
            assert!(buf.is_empty());
            assert_eq!(value, actual)
        }
        Err(e) => panic!("Codec round trip failed: {:?}", e),
    }
}

fn round_trip_err<B>(input: &[u8], expected: FromBytesError)
where
    B: PartialEq + Debug + ByteCodec,
{
    let buf = BytesMut::from(input);
    assert_eq!(B::try_from_reader(&mut buf.reader()), Err(expected));
}

#[test]
fn test_string() {
    round_trip_ok("abcd".to_string());
    round_trip_err::<String>(&[5], FromBytesError::InvalidMarker(Marker::FixPos(5)));
    round_trip_err::<String>(
        &[4, 1, 2, 3],
        FromBytesError::InvalidMarker(Marker::FixPos(4)),
    );

    let err = std::str::from_utf8(&[0, 159, 146, 150]).unwrap_err();
    round_trip_err::<String>(
        &[164, 0, 159, 146, 150],
        FromBytesError::Utf8(err.to_string()),
    );
}

#[test]
fn test_u8() {
    round_trip_ok(u8::MAX);
}

#[test]
fn test_i32() {
    round_trip_ok(i32::max_value());
}

#[test]
fn test_i64() {
    round_trip_ok(i64::max_value());
}

#[test]
fn test_u32() {
    round_trip_ok(u32::MAX);
}

#[test]
fn test_u64() {
    round_trip_ok(u64::MAX);
}

#[test]
fn test_f32() {
    round_trip_ok(f32::MAX);
}

#[test]
fn test_usize() {
    round_trip_ok(usize::max_value());
}

#[test]
fn test_isize() {
    round_trip_ok(isize::max_value());
}

#[test]
fn test_f64() {
    round_trip_ok(f64::MAX);
}

#[test]
fn test_non_zero_usize() {
    round_trip_ok(NonZeroUsize::new(13).unwrap());
}

#[test]
fn test_duration() {
    round_trip_ok(Duration::from_secs(5));
}

#[test]
fn vec() {
    round_trip_ok(vec![vec![1, 2, 3], vec![4, 5, 6], vec![7, 8, 9]]);
    round_trip_err::<Vec<i32>>(&[5], FromBytesError::InvalidMarker(Marker::FixPos(5)));
    round_trip_err::<Vec<Vec<i32>>>(&[5, 5], FromBytesError::InvalidMarker(Marker::FixPos(5)));
}

#[test]
fn test_bool() {
    round_trip_ok(true);
    round_trip_err::<bool>(&[], FromBytesError::InsufficientData);
    round_trip_err::<bool>(&[3], FromBytesError::InvalidMarker(Marker::FixPos(3)));
}

#[test]
fn test_map() {
    round_trip_ok(HashMap::from([(1, true), (2, false), (3, true)]));
    round_trip_ok(HashMap::from([
        (1, vec![1, 2, 3]),
        (2, vec![4, 5, 6]),
        (3, vec![7, 8, 9]),
    ]));
}

#[test]
fn test_array() {
    round_trip_ok([1, 2, 3]);
    round_trip_ok([
        vec![true, false, true],
        vec![false, true, false],
        vec![true, true, true],
    ])
}
