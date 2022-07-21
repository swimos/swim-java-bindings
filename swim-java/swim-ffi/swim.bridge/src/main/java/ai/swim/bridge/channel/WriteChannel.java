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

import ai.swim.bridge.buffer.Buffer;
import ai.swim.bridge.channel.exceptions.ChannelClosedException;
import ai.swim.bridge.channel.exceptions.InsufficientCapacityException;
import java.util.Arrays;
import static java.lang.Math.min;

public class WriteChannel extends ByteChannel {

  WriteChannel(long ptr, Buffer buffer, Object lock) {
    super(ptr, buffer, lock);
  }

  public void writeAll(byte[] bytes) throws InterruptedException {
    while (true) {
      synchronized (lock) {
        try {
          int wrote = write(bytes);
          if (wrote == 0) {
            break;
          }
          bytes = Arrays.copyOfRange(bytes, wrote, bytes.length);
        } catch (InsufficientCapacityException e) {
          lock.wait();
        }
      }
    }
  }

  public int write(byte[] bytes) {
    if (isClosed()) {
      throw new ChannelClosedException("Channel closed");
    }

    if (bytes == null) {
      throw new NullPointerException("Provided buffer is null");
    }

    if (bytes.length == 0) {
      return 0;
    }

    synchronized (lock) {
      int readIdx = readIdx();
      int writeIdx = writeIdx();
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

      setWriteIdx(newWriteOffset);
      lock.notify();

      return toWrite;
    }
  }

}
