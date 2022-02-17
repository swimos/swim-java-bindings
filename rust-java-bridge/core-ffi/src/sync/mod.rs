// Copyright 2015-2021 Swim Inc.
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

use std::sync::atomic::Ordering;

pub mod atomic_bool;
pub mod atomic_long;

fn ordering(i: i32) -> Ordering {
    if i == 0 {
        Ordering::Relaxed
    } else if i == 1 {
        Ordering::Release
    } else if i == 2 {
        Ordering::Acquire
    } else if i == 3 {
        Ordering::AcqRel
    } else if i == 4 {
        Ordering::SeqCst
    } else {
        panic!("Unknown ordering: {}", i)
    }
}
