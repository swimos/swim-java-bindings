use crate::method::JavaObjectMethodDef;

pub struct CountdownLatch;
impl CountdownLatch {
    pub const COUNTDOWN: JavaObjectMethodDef =
        JavaObjectMethodDef::new("java/util/concurrent/CountDownLatch", "countDown", "()V");
}

pub struct Consumer;
impl Consumer {
    pub const ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
        "java/util/function/Consumer",
        "accept",
        "(Ljava/lang/Object;)V",
    );
}

pub struct Throwable;
impl Throwable {
    pub const GET_CAUSE: JavaObjectMethodDef =
        JavaObjectMethodDef::new("java/lang/Throwable", "getCause", "()Ljava/lang/Throwable;");
    pub const GET_MESSAGE: JavaObjectMethodDef =
        JavaObjectMethodDef::new("java/lang/Throwable", "getMessage", "()Ljava/lang/String;");
}
