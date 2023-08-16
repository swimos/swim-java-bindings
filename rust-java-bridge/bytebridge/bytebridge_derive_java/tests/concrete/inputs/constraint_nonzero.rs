#[bytebridge]
pub struct Test {
    #[bytebridge(non_zero)]
    a: i32,
    b: i32,
}
