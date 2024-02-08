#[macro_export]
macro_rules! server_fn {
    ($(#[$attrs:meta])* pub fn $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        jvm_sys::ffi_fn!($(#[$attrs])*, "ai/swim/server/SwimServerException", Java_ai_swim_server, $name($env, $class, $($arg: $ty)*) $(-> $ret)? $body);
    };
}
