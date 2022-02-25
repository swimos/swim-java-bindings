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

package ai.swim.ffi.sync;

import ai.swim.ffi.JniRunner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RAtomicBoolTest extends JniRunner {

  @Test
  void testAtomicBoolLoad() {
    RAtomicBool atomicBoolTrue = RAtomicBool.create(true);

    assertTrue(atomicBoolTrue.load(Ordering.Relaxed));

    RAtomicBool atomicBoolFalse = RAtomicBool.create(false);
    assertFalse(atomicBoolFalse.load(Ordering.Relaxed));
  }

  @Test
  void testAtomicBoolStore() {
    RAtomicBool atomicBool = RAtomicBool.create(true);
    assertTrue(atomicBool.load(Ordering.Relaxed));

    atomicBool.store(false, Ordering.Relaxed);
    assertFalse(atomicBool.load(Ordering.Relaxed));

    atomicBool.store(true, Ordering.Relaxed);
    assertTrue(atomicBool.load(Ordering.Relaxed));
  }

}
