#[bytebridge(rename = "Superclass")]
pub enum Test {
    #[bytebridge(rename = "SubclassA")]
    AVar { a: u8, b: std::time::Duration },
    #[bytebridge(rename = "SubclassB")]
    BVar { c: u32, d: String },
}
