use byte_channel::{ByteReader, ByteWriter};
use futures_util::future::BoxFuture;
use jni::sys::JavaVM;
use std::marker::PhantomData;
use swim_api::downlink::{Downlink, DownlinkConfig, DownlinkKind};
use swim_api::error::DownlinkTaskError;
use swim_downlink::lifecycle::ValueDownlinkLifecycle;
use swim_model::address::Address;
use swim_model::Text;

struct FfiValueDownlinkModel<T, LC> {
    _pd: PhantomData<T>,
    lifecycle: LC,
    vm: JavaVM,
}

impl<T, LC> FfiValueDownlinkModel<T, LC> {
    pub fn new(lifecycle: LC, vm: JavaVM) -> FfiValueDownlinkModel<T, LC> {
        FfiValueDownlinkModel {
            _pd: Default::default(),
            lifecycle,
            vm,
        }
    }
}

impl<T, LC> Downlink for FfiValueDownlinkModel<T, LC> {
    fn kind(&self) -> DownlinkKind {
        DownlinkKind::Value
    }

    fn run(
        self,
        path: Address<Text>,
        config: DownlinkConfig,
        input: ByteReader,
        output: ByteWriter,
    ) -> BoxFuture<'static, Result<(), DownlinkTaskError>> {
        todo!()
    }

    fn run_boxed(
        self: Box<Self>,
        path: Address<Text>,
        config: DownlinkConfig,
        input: ByteReader,
        output: ByteWriter,
    ) -> BoxFuture<'static, Result<(), DownlinkTaskError>> {
        todo!()
    }
}
