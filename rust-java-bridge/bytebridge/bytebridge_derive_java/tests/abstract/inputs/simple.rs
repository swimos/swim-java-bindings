#[bytebridge]
pub enum Test {
    AVar { a: u8, b: std::time::Duration },
    BVar { c: u32, d: String },
}
