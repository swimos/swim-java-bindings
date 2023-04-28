#[bytebridge(rename = "Superclass")]
pub struct Test {
    #[bytebridge(rename = "fieldA")]
    a: u8,
    #[bytebridge(rename = "member_field_b")]
    b: std::time::Duration,
}
