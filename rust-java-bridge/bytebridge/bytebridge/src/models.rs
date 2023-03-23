/// Everything inside the macro's braces will be written back out directly as Rust code. With the
/// FFI attributes removed.
bytebridge! {
    /// Rust documentation
    #[derive(Debug, PartialEq, Eq, Hash, FfiCodec)]
    #[codec(doc = "Java doc")]
    pub struct Prop {
        timeout: i32,
        something: i64
    }
}
