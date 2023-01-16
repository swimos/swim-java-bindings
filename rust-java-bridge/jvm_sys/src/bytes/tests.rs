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

use std::mem::size_of;

use crate::bytes::Bytes;

const I32_LEN: usize = size_of::<i32>();

fn build_buf() -> [u8; 16] {
    let mut buf: [u8; 16] = [0; 16];
    for var in 1..=4 {
        let bytes: [u8; 4] = inc_u8(var);
        for (byte_idx, byte) in bytes.into_iter().enumerate() {
            let idx = ((var as usize - 1) * I32_LEN + byte_idx) as usize;
            buf[idx] = byte;
        }
    }

    buf
}

fn inc_u8(i: u8) -> [u8; 4] {
    assert!(i.checked_add(3).is_some());
    [i, i + 1, i + 2, i + 3]
}

#[test]
fn gets() {
    let buf: &mut [u8] = &mut build_buf();
    let bytes = unsafe { Bytes::from_parts(buf, 16) };

    for i in 1..=4 {
        let val = bytes.get_u32((i - 1) * I32_LEN);
        let expected = u32::from_ne_bytes(inc_u8(i as u8));
        assert_eq!(val, expected);
    }

    for i in 2..=4 {
        let val_array: [u8; 4] = bytes.array((i - 1) * I32_LEN);
        let val = u32::from_ne_bytes(val_array);
        let expected = u32::from_ne_bytes(inc_u8(i as u8));
        assert_eq!(val, expected);
    }
}

#[test]
fn sets() {
    let a: [u8; 16] = [1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    let b: [u8; 16] = [1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0, 0, 0, 0, 0];
    let c: [u8; 16] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0, 0, 0];
    let d: [u8; 16] = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16];
    let arrays = vec![a, b, c, d];

    let buf: &mut [u8] = &mut [0; 16];
    let mut bytes = unsafe { Bytes::from_parts(buf, 16) };

    for (i, array) in arrays.into_iter().enumerate() {
        let mut subset = [0; 4];
        let idx = i * I32_LEN;
        subset.copy_from_slice(&array[idx..(4 + idx)]);

        let val = i32::from_ne_bytes(subset);
        bytes.set_i32(i * I32_LEN, val);
        let actual = bytes.get_i32(i * I32_LEN);

        assert_eq!(val, actual);
        assert_eq!(buf, array);
    }
}

macro_rules! oob_test {
    ($test_get:tt, $test_set:tt, $get_func:tt, $set_func:tt) => {
        #[test]
        #[should_panic]
        fn $test_get() {
            let buf: &mut [u8] = &mut [0; 16];
            let bytes = unsafe { Bytes::from_parts(buf, 16) };
            bytes.$get_func(64);
        }

        #[test]
        #[should_panic]
        fn $test_set() {
            let buf: &mut [u8] = &mut [0; 16];
            let mut bytes = unsafe { Bytes::from_parts(buf, 16) };
            bytes.$set_func(64, 0);
        }
    };
}

oob_test!(oob_get_i32, oob_set_i32, get_i32, set_i32);
oob_test!(oob_get_i64, oob_set_i64, get_i64, set_i64);
oob_test!(oob_get_u32, oob_set_u32, get_u32, set_u32);
oob_test!(oob_get_u64, oob_set_u64, get_u64, set_u64);
oob_test!(oob_get_usize, oob_set_usize, get_usize, set_usize);
oob_test!(oob_get_isize, oob_set_isize, get_isize, set_isize);

#[test]
#[should_panic]
fn oob_slice() {
    let buf: &mut [u8] = &mut [0; 16];
    let bytes = unsafe { Bytes::from_parts(buf, 16) };
    unsafe {
        bytes.slice_mut(64, 1);
    }
}

#[test]
#[should_panic]
fn oob_array() {
    let buf: &mut [u8] = &mut [0; 16];
    let bytes = unsafe { Bytes::from_parts(buf, 16) };
    bytes.array::<4>(64);
}

#[test]
#[should_panic]
fn oob_copy_from_slice() {
    let buf: &mut [u8] = &mut [0; 16];
    let mut bytes = unsafe { Bytes::from_parts(buf, 16) };
    bytes.copy_from_slice(64, &[]);
}

#[test]
#[should_panic]
fn oob_copy_from_slice2() {
    let buf: &mut [u8] = &mut [0; 16];
    let mut bytes = unsafe { Bytes::from_parts(buf, 16) };
    bytes.copy_from_slice(14, &[1, 2, 3, 4, 5, 6, 7, 8]);
}

#[test]
fn set_slice() {
    let buf: &mut [u8] = &mut [0; 8];
    let mut bytes = unsafe { Bytes::from_parts(buf, 8) };

    let input: &mut [u8] = &mut [1, 2, 3, 4, 5];
    bytes.copy_from_slice(2, input);

    let expected = [0, 0, 1, 2, 3, 4, 5, 0];
    assert_eq!(expected, buf);
}
