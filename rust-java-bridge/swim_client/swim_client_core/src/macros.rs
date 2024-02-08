#[macro_export]
macro_rules! client_fn {
    ($(#[$attrs:meta])* pub fn $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        jvm_sys::ffi_fn!($(#[$attrs])*, "ai/swim/client/SwimClientException", Java_ai_swim_client, $name($env, $class, $($arg: $ty)*) $(-> $ret)? $body);
    };
}
