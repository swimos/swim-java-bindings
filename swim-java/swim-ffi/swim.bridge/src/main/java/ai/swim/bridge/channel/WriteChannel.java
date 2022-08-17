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
import ai.swim.bridge.channel.exceptions.ChannelClosedException;
import ai.swim.bridge.channel.exceptions.InsufficientCapacityException;

import java.lang.invoke.VarHandle;
import java.util.Arrays;

import static java.lang.Math.min;

// todo: add a 'awaitShutdown' method that awaits the Rust reader being dropped/draining the buffer after the channel
//  has been closed. This will be required to avoid the potential condition where the Java writer is closed and the Rust
//  reader has not had time to finish draining the buffer before the Java writer has been garbage collected. 
public class WriteChannel extends ByteChannel {

  WriteChannel(long ptr, HeapByteBuffer buffer, Object lock) {
    super(ptr, buffer, lock);
  }

  public void writeAll(byte[] bytes) throws InterruptedException {
    while (true) {
      try {
        int count = write(bytes);
        if (count == 0 || count == bytes.length) {
          break;
        }

        bytes = Arrays.copyOfRange(bytes, count, bytes.length);
      } catch (InsufficientCapacityException e) {
        synchronized (lock) {
          int writeIdx = buffer.getIntAcquire(WRITE);
          int readIdx = buffer.getIntOpaque(READ);
          int remaining = wrappingSub(readIdx, writeIdx + 1) % len() + 1;

          if (remaining > 1) {
            continue;
          }

          lock.wait();
        }
      }
    }
  }

  public int write(byte[] bytes) {
    if (bytes == null) {
      throw new NullPointerException("Provided buffer is null");
    }

    if (bytes.length == 0) {
      return 0;
    }

    if (isClosed()) {
      throw new ChannelClosedException("Channel closed");
    }

    int writeIdx = buffer.getIntAcquire(WRITE);
    int readIdx = buffer.getIntOpaque(READ);
    int remaining = wrappingSub(readIdx, writeIdx + 1) % len() + 1;

    if (remaining <= 1) {
      throw new InsufficientCapacityException("Channel full");
    }

    int toWrite = min(bytes.length, remaining - 1);
    int capped = min(toWrite, len() - writeIdx);
    int cursor = 0;

    for (int i = writeIdx; i < writeIdx + capped; i++) {
      buffer.setByte(idx(i), bytes[cursor++]);
    }

    int newWriteOffset;

    if (capped != toWrite) {
      int lim = toWrite - capped;

      for (int i = 0; i < lim; i++) {
        buffer.setByte(idx(i), bytes[cursor++]);
      }
      newWriteOffset = lim;
    } else {
      newWriteOffset = writeIdx + toWrite;
    }

    if (newWriteOffset == len()) {
      newWriteOffset = 0;
    }

    buffer.setIntRelease(WRITE, newWriteOffset);

    synchronized (lock) {
      lock.notify();
    }

    return toWrite;
  }

}
