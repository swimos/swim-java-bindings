#[bytebridge]
pub struct Test {
    #[bytebridge(default_value = "30s")]
    a: std::time::Duration,
    b: i32,
}
