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
use std::ptr::{copy_nonoverlapping, slice_from_raw_parts_mut, NonNull};

use jni::objects::JByteBuffer;
use jni::JNIEnv;

use crate::jvm_tryf;

#[cfg(test)]
mod tests;

/// A view in to a shared memory region that has been initialized by a Java process.
///
/// No synchronization is performed and the owner of this struct must ensure that the region of
/// memory being written to is locked. A bytes instance contains no internal read or write pointers
/// and instead takes a pointer to where to read and write from.

// Set operations intentionally take a mutable reference to self to prevent the data from being
// changed by multiple owners.
#[derive(Debug)]
pub struct Bytes {
    ptr: NonNull<u8>,
    capacity: usize,
}

macro_rules! fwd_impl {
    ($get:tt, $set:tt, $ty:ty) => {
        #[inline]
        pub fn $get(&self, base: usize) -> $ty {
            let mut bytes = [0; size_of::<$ty>()];
            check_bound(self.capacity, base, bytes.len());

            for i in 0..size_of::<$ty>() {
                bytes[i] = self.get_u8(base + i);
            }

            <$ty>::from_ne_bytes(bytes)
        }

        #[inline]
        pub fn $set(&mut self, base: usize, to: $ty) {
            let bytes = to.to_ne_bytes();
            check_bound(self.capacity, base, bytes.len());

            for i in 0..bytes.len() {
                unsafe {
                    *self.ptr.as_ptr().add(base + i) = *&bytes[i];
                }
            }
        }
    };
}

impl Bytes {
    #[cfg(test)]
    #[allow(clippy::missing_safety_doc)]
    pub unsafe fn from_parts(buf: &mut [u8], capacity: usize) -> Bytes {
        Bytes {
            ptr: NonNull::new_unchecked(buf.as_mut_ptr()),
            capacity,
        }
    }

    /// Creates a new byte view from 'buffer'. The provided buffer must have a capacity of less than
    /// u32::MAX.
    pub fn new(env: JNIEnv, buffer: JByteBuffer) -> Bytes {
        let buf = env
            .get_direct_buffer_address(buffer)
            .expect("Failed to get direct byte buffer address");
        let capacity = env
            .get_direct_buffer_capacity(buffer)
            .expect("Failed to get direct byte buffer capacity");
        // safe as get_direct_buffer_address checks for a nullptr
        let ptr = unsafe { NonNull::new_unchecked(buf.as_mut_ptr()) };

        if buf.len() < 2 {
            env.fatal_error("Cannot create a buffer with a capacity of less than 2")
        }

        let capacity = jvm_tryf! {
            env,
            format!("Cannot create a buffer with a capacity greater than u32::MAX: {} > {}", capacity, u32::MAX),
            u32::try_from(capacity)
        };

        Bytes {
            ptr,
            capacity: capacity as usize,
        }
    }

    #[inline]
    pub fn capacity(&self) -> usize {
        self.capacity
    }

    /// Returns a mutable raw pointer to 'count'.
    ///
    /// # Safety
    /// Panics if 'from + count' is out of bounds.
    #[inline]
    pub unsafe fn get_mut_u8(&self, count: usize) -> *mut u8 {
        check_bound(self.capacity, count, 1);
        self.ptr.as_ptr().add(count)
    }

    #[inline]
    pub unsafe fn get_mut_i32(&self, count: usize) -> *mut u8 {
        check_bound(self.capacity, count, 1);
        self.ptr.as_ptr().add(count)
    }

    #[inline]
    pub fn get_u8(&self, count: usize) -> u8 {
        check_bound(self.capacity, count, 1);
        unsafe { *self.get_mut_u8(count) }
    }

    fwd_impl!(get_i32, set_i32, i32);
    fwd_impl!(get_i64, set_i64, i64);
    fwd_impl!(get_u32, set_u32, u32);
    fwd_impl!(get_u64, set_u64, u64);
    fwd_impl!(get_usize, set_usize, usize);
    fwd_impl!(get_isize, set_isize, isize);

    /// Returns a mutable slice starting at 'from' to 'from + count'.
    ///
    /// # Safety
    /// Panics if 'from + count' is out of bounds.
    #[inline]
    pub unsafe fn slice_mut(&self, from: usize, count: usize) -> *mut [u8] {
        check_bound(self.capacity, from, count);
        slice_from_raw_parts_mut::<u8>(self.ptr.as_ptr().add(from), count)
    }

    #[inline]
    pub fn copy_from_slice(&mut self, start: usize, src: &[u8]) {
        check_bound(self.capacity, start, src.len());
        unsafe {
            copy_nonoverlapping(src.as_ptr(), self.ptr.as_ptr().add(start), src.len());
        }
    }

    #[inline]
    pub fn array<const N: usize>(&self, from: usize) -> [u8; N] {
        check_bound(self.capacity, from, N);
        let slice = unsafe { &*self.slice_mut(from, N) };
        let mut dest = [0; N];

        for (idx, b) in slice.iter().enumerate() {
            dest[idx] = *b;
        }

        dest
    }
}

#[inline]
fn check_bound(capacity: usize, from: usize, to: usize) {
    let lim = from + to;
    if lim > capacity {
        #[cold]
        #[inline(never)]
        fn panic_oob(capacity: usize, lim: usize) {
            panic!("Index {} is out of bounds of {}", lim, capacity);
        }

        panic_oob(capacity, lim);
    }
}
