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

package ai.swim.ffi.io;

import ai.swim.ffi.FfiIntrinsic;
import ai.swim.ffi.JniRunner;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;

class RByteReaderTest extends JniRunner {

  private static CountDownLatch readLatch = new CountDownLatch(6);
  private static CountDownLatch closeLatch = new CountDownLatch(1);

  @Test
  void read() throws InterruptedException {
    long now = System.nanoTime();

    RByteReader rByteReader = RByteReader.create(64,
        new DidReadImpl(),
        new DidCloseImpl()
    );

    readLatch.await();
    closeLatch.await();
    System.out.println(System.nanoTime() - now);
  }

  static class DidReadImpl implements RByteReader.DidReadCallback {
    @Override
    @FfiIntrinsic
    public void didRead(byte[] bytes) {
      readLatch.countDown();
    }
  }

  static class DidCloseImpl implements RByteReader.DidCloseCallback {
    @Override
    @FfiIntrinsic
    public void didClose() {
      System.out.println("Reader closed");
      closeLatch.countDown();
    }
  }

}