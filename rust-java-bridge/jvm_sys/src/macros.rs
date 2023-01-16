// Copyright 2015-2022 Swim Inc.
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
macro_rules! jni_try {
    ($env:ident, $class:tt, $msg:tt, $expr:expr $(,)?) => {{
        let env_ref = $env;

        match $expr {
            Ok(val) => val,
            Err(e) => {
                env_ref
                    .throw_new($class, format!("{}: {:?}", $msg, e))
                    .expect("Failed to throw exception");
                return;
            }
        }
    }};
    ($env:ident, $msg:tt, $expr:expr $(,)?) => {
        $crate::jni_try!($env, "java/lang/Exception", $msg, $expr)
    };
}
