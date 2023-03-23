use std::str::Utf8Error;

use bytes::BytesMut;

mod impls;

#[cfg(feature = "derive")]
pub use bytebridge_derive::*;

/// Trait for defining a transformation between an object and its byte representation.
pub trait ByteCodec {
    /// Attempt to decode an instance of this type from a buffer of bytes.
    fn try_from_bytes(bytes: &mut BytesMut) -> Result<Self, FromBytesError>
    where
        Self: Sized;

    /// Transform this instance into its byte representation.
    fn to_bytes(&self, bytes: &mut BytesMut);
}

/// Unrecoverable errors produced when attempting to decode an object from its byte representation.
#[derive(Debug, PartialEq)]
pub enum FromBytesError {
    /// An invalid UTF-8 encoding was read.
    Utf8(Utf8Error),
    /// An invalid boolean value was read.
    Bool(u8),
    /// There was insufficient data in the buffer.
    InsufficientData,
    /// Unknown enum variant found
    UnknownEnumVariant(u8),
}
