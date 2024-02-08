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

#[macro_export]
macro_rules! server_fn {
    ($(#[$attrs:meta])* pub fn $name:tt ($env:ident, $class:ident $(,)? $($arg:ident: $ty:ty $(,)?)*) $(-> $ret:tt)? $body:block) => {
        jvm_sys::ffi_fn!($(#[$attrs])*, "ai/swim/server/SwimServerException", Java_ai_swim_server, $name($env, $class, $($arg: $ty)*) $(-> $ret)? $body);
    };
}
