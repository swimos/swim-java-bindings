use std::str::Utf8Error;

pub use bytes::{BufMut, BytesMut};

mod impls;

#[cfg(feature = "derive_java")]
pub use bytebridge_derive_java::*;

#[doc(hidden)]
pub use bytebridge_derive::*;

pub use impls::has_size_of;

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
#[derive(Debug, PartialEq, Eq)]
pub enum FromBytesError {
    /// An invalid UTF-8 encoding was read.
    Utf8(Utf8Error),
    /// An invalid boolean value was read.
    Bool(u8),
    /// There was insufficient data in the buffer.
    InsufficientData,
    /// Unknown enum variant was read.
    UnknownEnumVariant(u8),
    /// Invalid data was read. The String contains the error's cause.
    Invalid(String),
}
