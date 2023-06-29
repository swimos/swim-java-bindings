#[bytebridge]
pub struct Test {
    #[bytebridge(unsigned_array)]
    a: Vec<i32>,
    b: i32,
}
