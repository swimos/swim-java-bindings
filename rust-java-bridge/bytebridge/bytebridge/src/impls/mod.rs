use std::mem::size_of;
use std::num::NonZeroUsize;
use std::time::Duration;

use bytes::{Buf, BufMut, BytesMut};

use crate::{ByteCodec, FromBytesError};

#[cfg(test)]
mod tests;

/// Returns Ok(()) if the buffer has at least `cnt` bytes available or
/// Err(FromBytesError::InsufficientData) if it does not.
pub fn has(bytes: &BytesMut, cnt: usize) -> Result<(), FromBytesError> {
    if bytes.remaining() >= cnt {
        Ok(())
    } else {
        Err(FromBytesError::InsufficientData)
    }
}

/// Returns Ok(()) if the buffer has at least `size_of::<T>()` bytes available or
/// Err(FromBytesError::InsufficientData) if it does not.
pub fn has_size_of<T>(bytes: &BytesMut) -> Result<(), FromBytesError> {
    has(bytes, size_of::<T>())
}

macro_rules! impl_bytes {
    ($ty:ty, encode => |$enc_self:ident, $enc_bytes:ident| $encode_block:block, decode => |$decode_bytes:ident| $decode_block:block $(,)?) => {
        impl ByteCodec for $ty {
            fn try_from_bytes($decode_bytes: &mut BytesMut) -> Result<Self, FromBytesError>
            where
                Self: Sized,
            {
                $decode_block
            }

            fn to_bytes(&$enc_self, $enc_bytes: &mut BytesMut) {
                $encode_block
            }
        }
    };
}

macro_rules! number_bytes {
    ($($ty:ty => ($encode:tt, $decode:tt) $(,)?)*) => {
        $(impl_bytes!(
            $ty,
            encode => |self, bytes| {
                bytes.reserve(size_of::<$ty>());
                bytes.$encode(*self);
            },
            decode => |bytes| {
                has(&bytes, size_of::<$ty>())?;
                Ok(bytes.$decode())
            }
        );)*
    };
}

number_bytes! {
    i8 => (put_i8, get_i8),
    i32 => (put_i32_le, get_i32_le),
    i64 => (put_i64_le, get_i64_le),
    u32 => (put_u32_le, get_u32_le),
    u64 => (put_u64_le, get_u64_le),
    f32 => (put_f32_le, get_f32_le),
    f64 => (put_f64_le, get_f64_le),
}

impl_bytes! {
    usize,
    encode => |self, bytes| {
        bytes.reserve(size_of::<u64>());
        bytes.put_u64_le(*self as u64);
    },
    decode => |bytes| {
        has(&bytes, size_of::<u64>())?;
        Ok(bytes.get_u64_le() as usize)
    }
}

impl_bytes! {
    isize,
    encode => |self, bytes| {
        bytes.reserve(size_of::<u64>());
        bytes.put_u64_le(*self as u64);
    },
    decode => |bytes| {
        has(&bytes, size_of::<i64>())?;
        Ok(bytes.get_i64_le() as isize)
    }
}

impl_bytes! {
    Duration,
    encode => |self, bytes| {
        bytes.reserve(size_of::<u64>());
        bytes.put_u64_le(self.as_secs());
    },
    decode => |bytes| {
        has(&bytes, size_of::<u64>())?;
        Ok(Duration::from_secs(bytes.get_u64_le()))
    }
}

impl_bytes! {
    NonZeroUsize,
    encode => |self, bytes| {
        usize::to_bytes(&self.get(), bytes)
    },
    decode => |bytes| {
        NonZeroUsize::new(usize::try_from_bytes(bytes)?).ok_or_else(||{
            todo!()
        })
    }
}

impl_bytes! {
    String,
    encode => |self, bytes| {
        let len = self.len();
        let data = self.as_bytes();
        bytes.reserve(size_of::<u64>() + data.len());
        bytes.put_u64_le(len as u64);
        bytes.extend_from_slice(data);
    },
    decode => |bytes| {
        has(&bytes, size_of::<u64>())?;
        let len = bytes.get_u64_le() as usize;

        has(&bytes, len)?;

        match std::str::from_utf8(&bytes[0..len]) {
            Ok(str) => {
                let string = str.to_string();
                bytes.advance(len);
                Ok(string)
            },
            Err(e) => Err(FromBytesError::Utf8(e))
        }
    }
}

impl_bytes! {
    u8,
    encode => |self, bytes| {
        bytes.reserve(size_of::<u8>());
        bytes.put_u8(*self);
    },
    decode => |bytes| {
        has(&bytes, size_of::<u8>())?;
        Ok(bytes.get_u8())
    }
}

impl_bytes! {
    bool,
    encode => |self, bytes| {
        u8::to_bytes(&(*self as u8), bytes);
    },
    decode => |bytes| {
        has(&bytes, size_of::<u8>())?;
        match u8::try_from_bytes(bytes){
            Ok(1) => Ok(true),
            Ok(0) => Ok(false),
            Ok(n) => Err(FromBytesError::Bool(n)),
            Err(e) => Err(e)
        }
    }
}

impl<B> ByteCodec for Vec<B>
where
    B: ByteCodec,
{
    fn try_from_bytes(bytes: &mut BytesMut) -> Result<Self, FromBytesError>
    where
        Self: Sized,
    {
        has(&bytes, size_of::<u64>())?;

        let element_count = bytes.get_u64_le();
        has(&bytes, element_count as usize)?;

        let mut collection = Vec::with_capacity(element_count as usize);

        for _ in 0..element_count {
            collection.push(B::try_from_bytes(bytes)?);
        }

        Ok(collection)
    }

    fn to_bytes(&self, bytes: &mut BytesMut) {
        let len = self.len();
        len.to_bytes(bytes);

        for x in self {
            x.to_bytes(bytes);
        }
    }
}
