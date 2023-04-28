use bytebridge::{ByteCodec, FromBytesError};
use bytes::BytesMut;
use std::fmt::Debug;

fn round_trip<O>(expected: O)
where
    O: ByteCodec + PartialEq + Debug,
{
    let mut buf = BytesMut::new();
    expected.to_bytes(&mut buf);

    match O::try_from_bytes(&mut buf) {
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

    let mut buf = BytesMut::from_iter([variant, 0, 0, 0, 2, 0, 0, 0]);
    match Prop::try_from_bytes(&mut buf) {
        Ok(_) => {
            panic!("Expected a test failure");
        }
        Err(FromBytesError::UnknownEnumVariant(n)) if n == variant => {}
        Err(e) => {
            panic!("Expected an unknown enum variant error. Got: {:?}", e);
        }
    }

    round_trip(Prop::VarA { a: 1, b: 2 });
}
