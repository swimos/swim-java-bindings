#[bytebridge]
pub struct Test {
    #[bytebridge(range(1, 20))]
    a: i32,
    b: i32,
}
