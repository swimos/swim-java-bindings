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

use jvm_sys::vm::method::JavaObjectMethodDef;

pub const ON_LINKED: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnLinked", "onLinked", "()V");
pub const ON_UNLINKED: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/lifecycle/OnUnlinked", "onUnlinked", "()V");
pub const CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "java/util/function/Consumer",
    "accept",
    "(Ljava/lang/Object;)V",
);
pub const BI_CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "java/util/function/BiConsumer",
    "accept",
    "(Ljava/lang/Object;Ljava/lang/Object;)V",
);
pub const TRI_CONSUMER_ACCEPT: JavaObjectMethodDef = JavaObjectMethodDef::new(
    "ai/swim/client/downlink/TriConsumer",
    "accept",
    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
);
pub const ROUTINE_EXEC: JavaObjectMethodDef =
    JavaObjectMethodDef::new("ai/swim/client/downlink/map/Routine", "exec", "()V");
