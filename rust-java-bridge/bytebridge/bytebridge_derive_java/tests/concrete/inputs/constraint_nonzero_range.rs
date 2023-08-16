#[bytebridge]
pub struct Test {
    #[bytebridge(range(1, 500))]
    a: std::num::NonZeroU32,
    b: i32,
}
