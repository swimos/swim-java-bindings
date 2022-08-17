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
import ai.swim.bridge.runtime.RuntimeProxy;

import java.nio.ByteBuffer;

// todo: add a 'awaitShutdown' method that awaits the Rust reader being dropped/draining the buffer after the channel
//  has been closed. This will be required to avoid the potential condition where the Java writer is closed and the Rust
//  reader has not had time to finish draining the buffer before the Java writer has been garbage collected.
public final class Channels {

  static {
    System.loadLibrary("swim_sys");
  }

  private Channels() {
    throw new AssertionError();
  }

  private static void checkCapacity(int capacity) {
    if (capacity < 2) {
      throw new IllegalArgumentException("Cannot create a channel with a capacity of less than 2");
    }
  }

  public static ReadChannel readChannel(int capacity) {
    checkCapacity(capacity);

    Object lock = new Object();
    HeapByteBuffer bufferView = new HeapByteBuffer(Math.addExact(capacity, ByteChannel.HEADER_SIZE));
    long writePtr = writeChannel(bufferView.rawBuffer(), lock);

    return new ReadChannel(writePtr, bufferView, lock);
  }

  public static WriteChannel writeChannel(int capacity) {
    checkCapacity(capacity);

    Object lock = new Object();
    HeapByteBuffer bufferView = new HeapByteBuffer(Math.addExact(capacity, ByteChannel.HEADER_SIZE));
    long writePtr = readChannel(bufferView.rawBuffer(), lock, RuntimeProxy.runtime().handle().getPointer());

    return new WriteChannel(writePtr, bufferView, lock);
  }

  private static native long writeChannel(ByteBuffer buffer, Object lock);

  private static native long readChannel(ByteBuffer buffer, Object lock, long runtimePtr);

}
