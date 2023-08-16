#[bytebridge]
pub struct Test {
    #[bytebridge(natural_number)]
    a: i32,
    b: i32,
}
