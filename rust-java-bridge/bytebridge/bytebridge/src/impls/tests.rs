use crate::{ByteCodec, FromBytesError};
use bytes::BytesMut;
use std::fmt::Debug;
use std::num::NonZeroUsize;
use std::time::Duration;
use std::usize;

fn round_trip_ok<B>(value: B)
where
    B: PartialEq + Debug + ByteCodec,
{
    let mut buf = BytesMut::new();
    value.to_bytes(&mut buf);

    match B::try_from_bytes(&mut buf) {
        Ok(actual) => {
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
    let mut buf = BytesMut::from(input);
    assert_eq!(B::try_from_bytes(&mut buf), Err(expected));
}

#[test]
fn test_string() {
    round_trip_ok("abcd".to_string());
    round_trip_err::<String>(&[5], FromBytesError::InsufficientData);
    round_trip_err::<String>(&[4, 1, 2, 3], FromBytesError::InsufficientData);

    let err = std::str::from_utf8(&[0, 159, 146, 150]).unwrap_err();
    round_trip_err::<String>(
        &[4, 0, 0, 0, 0, 0, 0, 0, 0, 159, 146, 150],
        FromBytesError::Utf8(err),
    );
}

#[test]
fn test_u8() {
    round_trip_ok(u8::MAX);
    round_trip_err::<u8>(&[], FromBytesError::InsufficientData);
}

#[test]
fn test_i32() {
    round_trip_ok(i32::MAX);
    round_trip_err::<i32>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_i64() {
    round_trip_ok(i64::MAX);
    round_trip_err::<i64>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_u32() {
    round_trip_ok(u32::MAX);
    round_trip_err::<u32>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_u64() {
    round_trip_ok(u64::MAX);
    round_trip_err::<u64>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_f32() {
    round_trip_ok(f32::MAX);
    round_trip_err::<f32>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_f64() {
    round_trip_ok(f64::MAX);
    round_trip_err::<f64>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_usize() {
    round_trip_ok(u32::MAX as usize);
    round_trip_err::<usize>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_isize() {
    round_trip_ok(u32::MAX as isize);
    round_trip_err::<isize>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_non_zero_usize() {
    round_trip_ok(NonZeroUsize::new(13).unwrap());
    round_trip_err::<NonZeroUsize>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn test_duration() {
    round_trip_ok(Duration::from_secs(5));
    round_trip_err::<Duration>(&[0, 0, 0], FromBytesError::InsufficientData);
}

#[test]
fn vec() {
    round_trip_ok(vec![vec![1, 2, 3], vec![4, 5, 6], vec![7, 8, 9]]);
    round_trip_err::<Vec<i32>>(&[5], FromBytesError::InsufficientData);
    round_trip_err::<Vec<Vec<i32>>>(&[5, 5], FromBytesError::InsufficientData);
}

#[test]
fn test_bool() {
    round_trip_ok(true);
    round_trip_err::<bool>(&[], FromBytesError::InsufficientData);
    round_trip_err::<bool>(&[3], FromBytesError::Bool(3));
}
