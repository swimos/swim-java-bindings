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

package ai.swim.ffi;

import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class PointerTest {
  private static final int resourceCount = 100;
  private static final AtomicInteger nativeObjectCount = new AtomicInteger(resourceCount);

  @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "ResultOfMethodCallIgnored"})
  public void pack() {
    List<int[]> data = new ArrayList<>();
    for (int i = 0; i < 128; ++i) {
      data.add(new int[64]);
    }
    data.toString();
  }

  @Test
  void freesResources() {
    Deque<NativeResource> objects = new ArrayDeque<>();

    while (true) {
      pack();

      objects.add(new Marker());

      if (objects.size() > resourceCount) {
        objects.removeFirst();
      }

      if (nativeObjectCount.get() <= 0) {
        break;
      }
    }
  }

  static class Marker implements NativeResource {
    @SuppressWarnings("unused")
    private final Pointer.Destructor destructor = Pointer.newDestructor(this, nativeObjectCount::decrementAndGet);
  }
}