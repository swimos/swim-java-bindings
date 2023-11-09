#[macro_export]
macro_rules! server_fn {
    (
        $(#[$attrs:meta])*
        fn $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block
    ) => {
        jvm_sys::ffi_fn!("ai/swim/server/SwimServerException", Java_ai_swim_server, $(#[$attrs])* fn $name($env, $class, $($arg: $ty)*) $(-> $ret)? $body);
    };
}
