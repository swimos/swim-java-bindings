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
    ($env:ident, $class:tt, $msg:tt, $expr:expr, $ret:expr $(,)?) => {{
        let env_ref = $env;

        match $expr {
            Ok(val) => val,
            Err(e) => {
                env_ref
                    .throw_new($class, format!("{}: {:?}", $msg, e))
                    .expect("Failed to throw exception");
                return $ret;
            }
        }
    }};
    ($env:ident, $class:tt, $msg:tt, $expr:expr, $(,)?) => {{
        $crate::jni_try!($env, $class, $msg, $expr, ())
    }};
    ($env:ident, $msg:tt, $expr:expr $(,)?) => {
        $crate::jni_try!($env, "ai/swim/client/SwimClientException", $msg, $expr, ())
    };
    ($env:ident, $msg:tt, $expr:expr, $ret:expr $(,)?) => {
        $crate::jni_try!(
            $env,
            "ai/swim/client/SwimClientException",
            $msg,
            $expr,
            $ret
        )
    };
}

#[macro_export]
macro_rules! parse_string {
    ($env:ident, $string:tt, $ret:expr) => {{
        let env_ref = $env;
        let string = $string;
        let err = format!("Failed to parse {}", stringify!(name));

        let java_string = jni_try! {
            env_ref,
            err,
            env_ref.get_string(string),
            $ret
        };
        jni_try! {
            env_ref,
            err,
            java_string.to_str(),
            $ret
        }
        .to_string()
    }};
    ($env:ident, $string:tt) => {{
        $crate::parse_string!($env, $string, ())
    }};
}
