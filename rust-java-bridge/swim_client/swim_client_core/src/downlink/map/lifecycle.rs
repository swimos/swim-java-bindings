// Copyright 2015-2024 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use jni::sys::{jint, jobject};
use swim_api::error::DownlinkTaskError;

use jvm_sys::env::{IsTypeOfExceptionHandler, JavaEnv};

use crate::downlink::vtable::{ExceptionHandler, JavaMethod};
use crate::downlink::{
    DISPATCH_DROP, DISPATCH_ON_CLEAR, DISPATCH_ON_REMOVE, DISPATCH_ON_UPDATE, DISPATCH_TAKE,
    ON_LINKED, ON_UNLINKED, ROUTINE_EXEC,
};

pub struct MapDownlinkLifecycle {
    vtable: MapDownlinkVTable,
}

impl MapDownlinkLifecycle {
    pub fn from_parts(
        env: &JavaEnv,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
    ) -> MapDownlinkLifecycle {
        MapDownlinkLifecycle {
            vtable: MapDownlinkVTable::new(
                env,
                on_linked,
                on_synced,
                on_update,
                on_remove,
                on_clear,
                on_unlinked,
                take,
                drop,
            ),
        }
    }

    pub fn on_linked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        vtable.on_linked(env)
    }

    pub fn on_synced(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        vtable.on_synced(env)
    }

    pub fn on_update(
        &mut self,
        env: &JavaEnv,
        key: &mut Vec<u8>,
        value: &mut Vec<u8>,
        dispatch: bool,
    ) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable } = self;
        vtable.on_update(env, key, value, dispatch)
    }

    pub fn on_remove(
        &mut self,
        env: &JavaEnv,
        key: &mut Vec<u8>,
        dispatch: bool,
    ) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable } = self;
        vtable.on_remove(env, key, dispatch)
    }

    pub fn on_clear(&mut self, env: &JavaEnv, dispatch: bool) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable } = self;
        vtable.on_clear(env, dispatch)
    }

    pub fn on_unlinked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable, .. } = self;
        vtable.on_unlinked(env)
    }

    pub fn take(
        &mut self,
        env: &JavaEnv,
        n: jint,
        dispatch: bool,
    ) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable } = self;
        vtable.take(env, n, dispatch)
    }

    pub fn drop(
        &mut self,
        env: &JavaEnv,
        n: jint,
        dispatch: bool,
    ) -> Result<(), DownlinkTaskError> {
        let MapDownlinkLifecycle { vtable } = self;
        vtable.drop(env, n, dispatch)
    }
}

pub struct MapDownlinkVTable {
    on_linked: JavaMethod,
    on_synced: JavaMethod,
    on_update: JavaMethod,
    on_remove: JavaMethod,
    on_clear: JavaMethod,
    on_unlinked: JavaMethod,
    take: JavaMethod,
    drop: JavaMethod,
    handler: ExceptionHandler,
}

impl MapDownlinkVTable {
    fn new(
        env: &JavaEnv,
        on_linked: jobject,
        on_synced: jobject,
        on_update: jobject,
        on_remove: jobject,
        on_clear: jobject,
        on_unlinked: jobject,
        take: jobject,
        drop: jobject,
    ) -> MapDownlinkVTable {
        MapDownlinkVTable {
            on_linked: JavaMethod::for_method(env, on_linked, ON_LINKED),
            on_synced: JavaMethod::for_method(env, on_synced, ROUTINE_EXEC),
            on_update: JavaMethod::for_method(env, on_update, DISPATCH_ON_UPDATE),
            on_remove: JavaMethod::for_method(env, on_remove, DISPATCH_ON_REMOVE),
            on_clear: JavaMethod::for_method(env, on_clear, DISPATCH_ON_CLEAR),
            on_unlinked: JavaMethod::for_method(env, on_unlinked, ON_UNLINKED),
            take: JavaMethod::for_method(env, take, DISPATCH_TAKE),
            drop: JavaMethod::for_method(env, drop, DISPATCH_DROP),
            handler: ExceptionHandler(IsTypeOfExceptionHandler::new(
                env,
                "ai/swim/client/downlink/DownlinkException",
            )),
        }
    }

    fn on_linked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable {
            on_linked, handler, ..
        } = self;
        env.with_env(|scope| on_linked.execute(handler, &scope, &[]))
    }

    fn on_synced(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable {
            on_synced, handler, ..
        } = self;
        env.with_env(|scope| on_synced.execute(handler, &scope, &[]))
    }

    fn on_update(
        &mut self,
        env: &JavaEnv,
        key: &mut Vec<u8>,
        value: &mut Vec<u8>,
        dispatch: bool,
    ) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable {
            on_update, handler, ..
        } = self;
        env.with_env(|scope| {
            let key = scope.new_direct_byte_buffer_exact(key);
            let value = scope.new_direct_byte_buffer_exact(value);
            on_update.execute(
                handler,
                &scope,
                &[key.into(), value.into(), dispatch.into()],
            )
        })
    }

    fn on_remove(
        &mut self,
        env: &JavaEnv,
        key: &mut Vec<u8>,
        dispatch: bool,
    ) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable {
            on_remove, handler, ..
        } = self;
        env.with_env(|scope| {
            let key = scope.new_direct_byte_buffer_exact(key);
            on_remove.execute(handler, &scope, &[key.into(), dispatch.into()])
        })
    }

    fn on_clear(&mut self, env: &JavaEnv, dispatch: bool) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable {
            on_clear, handler, ..
        } = self;
        env.with_env(|scope| on_clear.execute(handler, &scope, &[dispatch.into()]))
    }

    fn on_unlinked(&mut self, env: &JavaEnv) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable {
            on_unlinked,
            handler,
            ..
        } = self;
        env.with_env(|scope| on_unlinked.execute(handler, &scope, &[]))
    }

    fn take(&mut self, env: &JavaEnv, n: jint, dispatch: bool) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable { take, handler, .. } = self;
        env.with_env(|scope| take.execute(handler, &scope, &[n.into(), dispatch.into()]))
    }

    fn drop(&mut self, env: &JavaEnv, n: jint, dispatch: bool) -> Result<(), DownlinkTaskError> {
        let MapDownlinkVTable { drop, handler, .. } = self;
        env.with_env(|scope| drop.execute(handler, &scope, &[n.into(), dispatch.into()]))
    }
}
