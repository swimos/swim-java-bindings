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

use byteorder::{BigEndian, ReadBytesExt};
use std::collections::HashMap;
use std::hash::Hash;
use std::io::{Error, Read, Write};
use std::mem::{forget, transmute_copy, MaybeUninit};
use std::num::NonZeroUsize;
use std::time::Duration;

use rmp::decode::{
    read_array_len, read_bool, read_f32, read_f64, read_i64, read_map_len, read_marker, read_u64,
};
use rmp::encode::{
    write_array_len, write_bool, write_f32, write_f64, write_i16, write_i32, write_i64, write_i8,
    write_map_len, write_str, write_u16, write_u32, write_u64, write_u8,
};
use rmp::Marker;

use crate::{ByteCodec, FromBytesError};

impl ByteCodec for i8 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => {
                let val = ReadBytesExt::read_u8(reader)?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U16 => {
                let val = reader.read_u16::<BigEndian>()?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U32 => {
                let val = reader.read_u32::<BigEndian>()?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => Ok(reader.read_i8()?),
            Marker::I16 => {
                let val = reader.read_i16::<BigEndian>()?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I32 => {
                let val = reader.read_i32::<BigEndian>()?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => i8::try_from(val).map_err(|_| FromBytesError::num_overflow(val)),
            Marker::FixNeg(val) => Ok(val),
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_i8(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for i16 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => {
                let val = ReadBytesExt::read_u8(reader)?;
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U16 => {
                let val = reader.read_u16::<BigEndian>()?;
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U32 => {
                let val = reader.read_u32::<BigEndian>()?;
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => Ok(reader.read_i8()? as i16),
            Marker::I16 => Ok(reader.read_i16::<BigEndian>()?),
            Marker::I32 => {
                let val = reader.read_i32::<BigEndian>()?;
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => {
                i16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixNeg(val) => Ok(val as i16),
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_i16(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for i32 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => {
                let val = ReadBytesExt::read_u8(reader)?;
                i32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U16 => {
                let val = reader.read_u16::<BigEndian>()?;
                i32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U32 => {
                let val = reader.read_u32::<BigEndian>()?;
                i32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                i32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => Ok(reader.read_i8()? as i32),
            Marker::I16 => Ok(reader.read_i16::<BigEndian>()? as i32),
            Marker::I32 => Ok(reader.read_i32::<BigEndian>()?),
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                i32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => {
                i32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixNeg(val) => Ok(val as i32),
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_i32(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for i64 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => {
                let val = ReadBytesExt::read_u8(reader)?;
                i64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U16 => {
                let val = reader.read_u16::<BigEndian>()?;
                i64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U32 => {
                let val = reader.read_u32::<BigEndian>()?;
                i64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                i64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => Ok(reader.read_i8()? as i64),
            Marker::I16 => Ok(reader.read_i16::<BigEndian>()? as i64),
            Marker::I32 => Ok(reader.read_i32::<BigEndian>()? as i64),
            Marker::I64 => Ok(reader.read_i64::<BigEndian>()?),
            Marker::FixPos(val) => {
                i64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixNeg(val) => Ok(val as i64),
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_i64(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for u8 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => Ok(ReadBytesExt::read_u8(reader)?),
            Marker::U16 => {
                let val = reader.read_u16::<BigEndian>()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U32 => {
                let val = reader.read_u32::<BigEndian>()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => {
                let val = reader.read_i8()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I16 => {
                let val = reader.read_i16::<BigEndian>()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I32 => {
                let val = reader.read_i32::<BigEndian>()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => Ok(val),
            Marker::FixNeg(val) => u8::try_from(val).map_err(|_| FromBytesError::num_overflow(val)),
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_u8(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for u16 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => Ok(ReadBytesExt::read_u8(reader)? as u16),
            Marker::U16 => Ok(reader.read_u16::<BigEndian>()?),
            Marker::U32 => {
                let val = reader.read_u32::<BigEndian>()?;
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => {
                let val = reader.read_i8()?;
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I16 => {
                let val = reader.read_i16::<BigEndian>()?;
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I32 => {
                let val = reader.read_i32::<BigEndian>()?;
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => Ok(val as u16),
            Marker::FixNeg(val) => {
                u16::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_u16(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for u32 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => Ok(ReadBytesExt::read_u8(reader)? as u32),
            Marker::U16 => Ok(reader.read_u16::<BigEndian>()? as u32),
            Marker::U32 => Ok(reader.read_u32::<BigEndian>()?),
            Marker::U64 => {
                let val = reader.read_u64::<BigEndian>()?;
                u32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I8 => {
                let val = reader.read_i8()?;
                u32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I16 => {
                let val = reader.read_i16::<BigEndian>()?;
                u32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I32 => {
                let val = reader.read_i32::<BigEndian>()?;
                u32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                u32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => Ok(val as u32),
            Marker::FixNeg(val) => {
                u32::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_u32(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for u64 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        match read_marker(reader)? {
            Marker::U8 => Ok(ReadBytesExt::read_u8(reader)? as u64),
            Marker::U16 => Ok(reader.read_u16::<BigEndian>()? as u64),
            Marker::U32 => Ok(reader.read_u32::<BigEndian>()? as u64),
            Marker::U64 => Ok(reader.read_u64::<BigEndian>()?),
            Marker::I8 => {
                let val = reader.read_i8()?;
                u64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I16 => {
                let val = reader.read_i16::<BigEndian>()?;
                u64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I32 => {
                let val = reader.read_i32::<BigEndian>()?;
                u64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::I64 => {
                let val = reader.read_i64::<BigEndian>()?;
                u64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            Marker::FixPos(val) => Ok(val as u64),
            Marker::FixNeg(val) => {
                u64::try_from(val).map_err(|_| FromBytesError::num_overflow(val))
            }
            m => Err(FromBytesError::InvalidMarker(m)),
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_u64(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for f32 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        Ok(read_f32(reader)?)
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_f32(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for f64 {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        Ok(read_f64(reader)?)
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_f64(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for bool {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        Ok(read_bool(reader)?)
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_bool(writer, *self)?;
        Ok(())
    }
}

impl ByteCodec for String {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        let len = match read_marker(reader)? {
            Marker::FixStr(len) => len as u64,
            Marker::Str8 => ReadBytesExt::read_u8(reader)? as u64,
            Marker::Str16 => reader.read_u16::<BigEndian>()? as u64,
            Marker::Str32 => reader.read_u32::<BigEndian>()? as u64,
            m => return Err(FromBytesError::InvalidMarker(m)),
        };

        let mut buf = vec![];
        reader.take(len).read_to_end(buf.as_mut())?;

        Ok(String::from_utf8(buf)?)
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_str(writer, self.as_str())?;
        Ok(())
    }
}

impl<E> ByteCodec for Vec<E>
where
    E: ByteCodec,
{
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        let len = read_array_len(reader)? as usize;
        let mut vec = Vec::with_capacity(len);
        for _ in 0..len {
            vec.push(E::try_from_reader(reader)?);
        }

        Ok(vec)
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_array_len(writer, self.len() as u32)?;

        for elem in self {
            elem.to_bytes(writer)?;
        }

        Ok(())
    }
}

impl ByteCodec for Duration {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        Ok(Duration::from_secs(u64::try_from_reader(reader)?))
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        u64::to_bytes(&self.as_secs(), writer)?;
        Ok(())
    }
}

impl ByteCodec for usize {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        #[cfg(target_pointer_width = "32")]
        {
            Ok(read_u32(reader)? as usize)
        }
        #[cfg(target_pointer_width = "64")]
        {
            Ok(read_u64(reader)? as usize)
        }
        #[cfg(not(any(target_pointer_width = "32", target_pointer_width = "64")))]
        {
            compile_error!("Unsupported pointer width")
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        #[cfg(target_pointer_width = "32")]
        {
            Ok(write_u32(writer, self as u32)?)
        }
        #[cfg(target_pointer_width = "64")]
        {
            Ok(write_u64(writer, *self as u64)?)
        }
        #[cfg(not(any(target_pointer_width = "32", target_pointer_width = "64")))]
        {
            compile_error!("Unsupported pointer width")
        }
    }
}

impl ByteCodec for isize {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        #[cfg(target_pointer_width = "32")]
        {
            Ok(read_i32(reader)? as isize)
        }
        #[cfg(target_pointer_width = "64")]
        {
            Ok(read_i64(reader)? as isize)
        }
        #[cfg(not(any(target_pointer_width = "32", target_pointer_width = "64")))]
        {
            compile_error!("Unsupported pointer width")
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        #[cfg(target_pointer_width = "32")]
        {
            Ok(write_i32(writer, self as i32)?)
        }
        #[cfg(target_pointer_width = "64")]
        {
            Ok(write_i64(writer, *self as i64)?)
        }
        #[cfg(not(any(target_pointer_width = "32", target_pointer_width = "64")))]
        {
            compile_error!("Unsupported pointer width")
        }
    }
}

impl ByteCodec for NonZeroUsize {
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        NonZeroUsize::try_from(usize::try_from_reader(reader)?)
            .map_err(|e| FromBytesError::Invalid(e.to_string()))
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        usize::to_bytes(&self.get(), writer)
    }
}

impl<K, V> ByteCodec for HashMap<K, V>
where
    K: ByteCodec + Eq + Hash,
    V: ByteCodec,
{
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        let len = read_map_len(reader)?;
        let mut map = HashMap::with_capacity(len as usize);

        for _ in 0..len {
            map.insert(K::try_from_reader(reader)?, V::try_from_reader(reader)?);
        }

        Ok(map)
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_map_len(writer, self.len() as u32)?;

        for (k, v) in self {
            k.to_bytes(writer)?;
            v.to_bytes(writer)?;
        }

        Ok(())
    }
}

impl<E, const N: usize> ByteCodec for [E; N]
where
    E: ByteCodec,
{
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read,
    {
        let len = read_array_len(reader)? as usize;
        if len != N {
            Err(FromBytesError::Invalid(format!(
                "Invalid array len. Expected {} got {}",
                N, len
            )))
        } else {
            let mut array = unsafe { MaybeUninit::<[MaybeUninit<E>; N]>::uninit().assume_init() };

            for i in 0..len {
                match E::try_from_reader(reader) {
                    Ok(e) => array[i] = MaybeUninit::new(e),
                    Err(e) => {
                        for maybe in array.into_iter().take(i) {
                            unsafe {
                                maybe.assume_init();
                            }
                        }

                        return Err(e);
                    }
                }
            }

            let init_array = unsafe { transmute_copy(&array) };
            forget(array);

            Ok(init_array)
        }
    }

    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write,
    {
        write_array_len(writer, N as u32)?;

        for e in self {
            e.to_bytes(writer)?;
        }

        Ok(())
    }
}
