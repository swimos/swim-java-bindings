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

package ai.swim.bridge.channel;

import ai.swim.bridge.HeapByteBuffer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ByteChannel {

  public static final int HEADER_SIZE = 12;
  private static final int CLOSED = 0;
  protected static final int READ = Integer.SIZE / Byte.SIZE;
  protected static final int WRITE = 2 * (Integer.SIZE / Byte.SIZE);
  protected static final int DATA = 3 * (Integer.SIZE / Byte.SIZE);
  protected final HeapByteBuffer buffer;
  protected final Object lock;
  private final long ptr;

  protected ByteChannel(long ptr, HeapByteBuffer buffer, Object lock) {
    this.ptr = ptr;
    this.buffer = buffer;
    this.lock = lock;
  }

  protected static int wrappingSub(int x, int y) {
    int r = x - y;
    if (r < 0) {
      r = Integer.MAX_VALUE - Math.abs(r + 1);
    }

    return r;
  }

  public boolean isClosed() {
    return buffer.getIntAcquire(CLOSED) == 1;
  }

  public void close() {
    buffer.setIntRelease(CLOSED, 1);

    synchronized (lock) {
      lock.notify();
    }
  }

  protected int idx(int from) {
    return DATA + from;
  }

  protected int len() {
    return buffer.capacity() - HEADER_SIZE;
  }
}

