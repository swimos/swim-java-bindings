pub struct ReactiveBridgeBuffer {
    on_write: fn() -> (),
}

impl ReactiveBridgeBuffer {
    pub fn write<A>(&mut self, buf: A) -> Result<(), ()>
    where
        A: AsRef<[u8]>,
    {
        let ReactiveBridgeBuffer { on_write } = self;
        inner.extend_from_slice(buf.as_ref())?;
        on_write();
        Ok(())
    }

    pub fn read<A>(&mut self, into: A) -> Result<(), ()>
    where
        A: AsMut<[u8]>,
    {
        unimplemented!()
    }
}
