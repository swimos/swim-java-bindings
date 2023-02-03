#[macro_export]
macro_rules! client_fn {
    ($name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        ffi_fn!("ai/swim/client/SwimClientException", Java_ai_swim_client, $name($env, $class, $($arg: $ty)*) $(-> $ret)? $body);
    };
}
