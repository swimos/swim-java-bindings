use std::time::Duration;

#[derive(Debug)]
#[bytebridge(doc = "Class documentation line 1", doc = "Class documentation line 2")]
struct BVar {
    #[bytebridge(doc = "name Rust doc")]
    pub name: String,
    #[bytebridge(doc = "timeout Rust doc")]
    pub timeout: Duration,
    /// Boolean
    pub booleeeeean: bool,
}
