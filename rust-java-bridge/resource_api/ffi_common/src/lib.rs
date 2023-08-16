pub struct FfiHandle<To> {
    inner: To,
    drop: fn(&mut To),
}

impl<To> FfiHandle<To> {
    pub fn new(inner: To, drop: fn(&mut To)) -> FfiHandle<To> {
        FfiHandle { inner, drop }
    }
}

impl<To> Drop for FfiHandle<To> {
    fn drop(&mut self) {
        let FfiHandle { inner, drop } = self;
        drop(inner)
    }
}
