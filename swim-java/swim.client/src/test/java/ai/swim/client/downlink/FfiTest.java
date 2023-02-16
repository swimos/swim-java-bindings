// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.client.downlink;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class FfiTest {
  static {
    System.loadLibrary("swim_client_test");
  }

  public static native void dropSwimClient(long ptr);

  public static native void dropRuntime(long ptr);

  public void awaitLatch(CountDownLatch latch, long time, String latchName) throws InterruptedException {
    if (!latch.await(time, TimeUnit.SECONDS)) {
      fail(String.format("%s latch elapsed before countdown reached", latchName));
    }
  }

  public enum LinkState {
    Init,
    Linked,
    Synced,
    Unlinked
  }
}
