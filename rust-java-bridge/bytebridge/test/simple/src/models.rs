// #[derive(Debug)]
// #[bytebridge(doc = "Class documentation line 1", doc = "Class documentation line 2")]
// pub struct BVar {
//     #[bytebridge(doc = "name Rust doc")]
//     pub name: String,
//     #[bytebridge(doc = "timeout Rust doc")]
//     pub timeout: std::time::Duration,
//     /// Boolean
//     pub booleeeeean: bool,
// }

#[bytebridge]
#[derive(Debug, Clone, PartialEq)]
pub enum EnumVar {
    #[bytebridge(rename = "AVarClass")]
    AVar {
        a: i32,
        b: i32,
    },
    BVar {
        c: i32,
        d: i32,
    },
}
