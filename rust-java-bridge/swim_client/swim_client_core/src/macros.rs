#[macro_export]
macro_rules! client_fn {
    (fn $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        jvm_sys::ffi_fn!("ai/swim/client/SwimClientException", Java_ai_swim_client, fn $name($env, $class, $($arg: $ty)*) $(-> $ret)? $body);
    };
}
