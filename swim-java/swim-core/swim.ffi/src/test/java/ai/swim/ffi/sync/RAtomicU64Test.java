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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class RAtomicU64Test extends JniRunner {

  @Test
  void testAtomicU64Load() {
    RAtomicU64 atomicU64 = RAtomicU64.create(64);
    assertEquals(atomicU64.load(Ordering.Relaxed), 64);
  }

  @Test
  void testAtomicU64Store() {
    RAtomicU64 atomicU64 = RAtomicU64.create(8);
    assertEquals(atomicU64.load(Ordering.Relaxed), 8);

    atomicU64.store(16, Ordering.Relaxed);
    assertEquals(atomicU64.load(Ordering.Relaxed), 16);
  }

  @Test
  void testIllegalArgumentCreate() {
    try {
      RAtomicU64.create(-8);
    } catch (IllegalArgumentException e) {
      return;
    }

    fail("Expected an illegal argument exception to be thrown when creating an RAtomicU64 with a negative number");
  }

  @Test
  void testIllegalArgumentStore() {
    RAtomicU64 atomicU64 = RAtomicU64.create(8);
    assertEquals(atomicU64.load(Ordering.Relaxed), 8);

    try {
      atomicU64.store(-8, Ordering.Relaxed);
    } catch (IllegalArgumentException e) {
      return;
    }

    fail("Expected an illegal argument exception to be thrown when storing a negative value in an RAtomicU64");
  }
}
