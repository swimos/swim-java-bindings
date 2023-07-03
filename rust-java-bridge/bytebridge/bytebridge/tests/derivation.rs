use bytebridge::{ByteCodec, FromBytesError};
use bytes::{Buf, BufMut, BytesMut};
use std::fmt::Debug;

fn round_trip<O>(expected: O)
where
    O: ByteCodec + PartialEq + Debug,
{
    let buf = BytesMut::new();
    let mut writer = buf.writer();
    expected
        .to_bytes(&mut writer)
        .expect("Failed to write bytes");

    let mut reader = writer.into_inner().reader();

    match O::try_from_reader(&mut reader) {
        Ok(actual) => {
            assert_eq!(expected, actual)
        }
        Err(e) => {
            panic!("Test failed: {:?}", e);
        }
    }
}

#[test]
fn struct_round_trip() {
    #[derive(Debug, ByteCodec, PartialEq, Eq)]
    pub struct Prop {
        pub a: i32,
        pub b: i32,
    }

    round_trip(Prop { a: 1, b: 2 });
}

#[test]
fn enum_round_trip() {
    #[derive(Debug, ByteCodec, PartialEq, Eq)]
    pub enum Prop {
        VarA { a: i32, b: i32 },
        VarB { c: i32, d: i32 },
    }

    round_trip(Prop::VarA { a: 1, b: 2 });
}

#[test]
fn unknown_enum_variant() {
    #[derive(Debug, ByteCodec, PartialEq, Eq)]
    pub enum Prop {
        VarA { a: i32, b: i32 },
    }

    let variant = 13;

    let mut buf =
        BytesMut::from_iter([212, 1, 208, variant, 146, 210, 0, 0, 0, 1, 210, 0, 0, 0, 2]).reader();
    match Prop::try_from_reader(&mut buf) {
        Ok(_) => {
            panic!("Expected a test failure");
        }
        Err(FromBytesError::UnknownEnumVariant(n)) if n == variant as i8 => {}
        Err(e) => {
            panic!("Expected an unknown enum variant error. Got: {:?}", e);
        }
    }

    round_trip(Prop::VarA { a: 1, b: 2 });
}
