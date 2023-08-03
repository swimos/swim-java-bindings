mod impls;
#[cfg(test)]
mod tests;

use byteorder::ReadBytesExt;
use bytes::Buf;
use std::any::{type_name, Any};
use std::fmt::Display;
use std::io::{Error, ErrorKind, Read, Write};
use std::string::FromUtf8Error;

pub use bytes::BytesMut;
pub use rmp::decode::read_array_len;
use rmp::decode::{read_marker, MarkerReadError, ValueReadError};
pub use rmp::encode::write_array_len;
use rmp::encode::write_ext_meta;
use rmp::Marker;

#[doc(hidden)]
pub use bytebridge_derive::*;
#[cfg(feature = "derive_java")]
pub use bytebridge_derive_java::*;

pub const ENUM_EXT: i8 = 1;

/// Trait for defining a transformation between an object and its byte representation.
pub trait ByteCodec {
    /// Attempt to decode an instance of this type from a reader.
    fn try_from_reader<R>(reader: &mut R) -> Result<Self, FromBytesError>
    where
        Self: Sized,
        R: Read;

    /// Attempt to decode an instance of this type from a buffer of bytes.
    fn try_from_bytes(bytes: &mut BytesMut) -> Result<Self, FromBytesError>
    where
        Self: Sized,
    {
        let mut reader = bytes.reader();
        Self::try_from_reader(&mut reader)
    }

    /// Transform this instance into its byte representation.
    fn to_bytes<W>(&self, writer: &mut W) -> Result<(), Error>
    where
        W: Write;
}

/// Unrecoverable errors produced when attempting to decode an object from its byte representation.
#[derive(Debug, PartialEq)]
pub enum FromBytesError {
    InvalidMarker(Marker),
    /// An invalid UTF-8 encoding was read.
    Utf8(String),
    /// An invalid boolean value was read.
    Bool(u8),
    /// There was insufficient data in the buffer.
    InsufficientData,
    /// Unknown enum variant was read.
    UnknownEnumVariant(i8),
    /// Integer value overflowed target type
    NumberOverflow(String),
    /// Invalid data was read. The String contains the error's cause.
    Invalid(String),
    Io {
        kind: ErrorKind,
        message: String,
    },
}

impl FromBytesError {
    pub fn num_overflow<A>(val: A) -> FromBytesError
    where
        A: Any + Display,
    {
        let type_name = type_name::<A>();
        FromBytesError::NumberOverflow(format!("Number overflow for {type_name}: {val}"))
    }
}

impl From<ValueReadError> for FromBytesError {
    fn from(value: ValueReadError) -> Self {
        match value {
            ValueReadError::InvalidMarkerRead(_) => FromBytesError::InsufficientData,
            ValueReadError::InvalidDataRead(_) => FromBytesError::InsufficientData,
            ValueReadError::TypeMismatch(marker) => FromBytesError::InvalidMarker(marker),
        }
    }
}

impl From<MarkerReadError> for FromBytesError {
    fn from(value: MarkerReadError) -> Self {
        let cause = value.0;
        FromBytesError::Io {
            kind: cause.kind(),
            message: cause.to_string(),
        }
    }
}

impl From<Error> for FromBytesError {
    fn from(value: Error) -> Self {
        FromBytesError::Io {
            kind: value.kind(),
            message: value.to_string(),
        }
    }
}

impl From<FromUtf8Error> for FromBytesError {
    fn from(value: FromUtf8Error) -> Self {
        FromBytesError::Utf8(value.to_string())
    }
}

pub fn read_ordinal<R>(reader: &mut R) -> Result<i8, FromBytesError>
where
    R: Read,
{
    match read_marker(reader)? {
        Marker::FixExt1 => {
            let ty = reader.read_i8()?;
            if ty == ENUM_EXT {
                let ordinal = i8::try_from_reader(reader)?;

                if (0..i8::max_value()).contains(&ordinal) {
                    Ok(ordinal)
                } else {
                    Err(FromBytesError::Invalid(format!(
                        "Invalid ordinal: {ordinal}"
                    )))
                }
            } else {
                Err(FromBytesError::Invalid(format!("Invalid extension: {ty}")))
            }
        }
        m => {
            println!("Hereeee");
            Err(FromBytesError::InvalidMarker(m))
        }
    }
}

pub fn write_ordinal<W>(writer: &mut W, ordinal: i8) -> Result<(), Error>
where
    W: Write,
{
    assert!(
        (0..i8::max_value()).contains(&ordinal),
        "Ordinal out of range"
    );

    write_ext_meta(writer, 1, ENUM_EXT)?;
    ordinal.to_bytes(writer)
}
