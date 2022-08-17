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

import static java.lang.Math.min;

public class ReadChannel extends ByteChannel {

  ReadChannel(long ptr, HeapByteBuffer buffer, Object lock) {
    super(ptr, buffer, lock);
  }

  public void readAll(byte[] into) throws InterruptedException {
    int cursor = 0;
    while (cursor != into.length) {
      byte[] buf = new byte[Math.min(len(), into.length - cursor)];
      int read = tryRead(buf);

      if (read == 0) {
        synchronized (lock) {
          int readIdx = buffer.getIntOpaque(READ);
          int writeIdx = buffer.getIntAcquire(WRITE);
          int available = wrappingSub(writeIdx, readIdx) % len();

          if (available != 0) {
            continue;
          }

          lock.wait();
        }
      } else {
        System.arraycopy(buf, 0, into, cursor, buf.length);
        cursor += read;
      }
    }
  }

  // todo: both this and the tryWrite op should accept a base index to read/write from so the array copies can be removed
  public int tryRead(byte[] into) throws InterruptedException {
    if (into == null) {
      throw new NullPointerException("Provided buffer is null");
    }

    if (into.length == 0) {
      return 0;
    }

    int readIdx = buffer.getIntOpaque(READ);
    int writeIdx = buffer.getIntAcquire(WRITE);
    int available = wrappingSub(writeIdx, readIdx) % len();

    if (available == 0) {
      if (isClosed()) {
        readIdx = buffer.getIntOpaque(READ);
        writeIdx = buffer.getIntAcquire(WRITE);
        available = wrappingSub(writeIdx, readIdx) % len();

        if (available != 0) {
          return tryRead(into);
        }

        throw new ChannelClosedException("Channel closed");
      } else {
        return 0;
      }
    }

    int toRead = min(available, into.length);
    int capped = min(toRead, len() - readIdx);
    int cursor = 0;

    for (int i = readIdx; i < readIdx + capped; i++) {
      into[cursor++] = buffer.getByte(idx(i));
    }

    int newReadOffset;

    if (capped != toRead) {
      int lim = toRead - capped;
      for (int i = 0; i < lim; i++) {
        into[cursor++] = buffer.getByte(idx(i));
      }
      newReadOffset = lim;
    } else {
      newReadOffset = readIdx + toRead;
    }

    if (newReadOffset == len()) {
      newReadOffset = 0;
    }

    buffer.setIntRelease(READ, newReadOffset);

    synchronized (lock) {
      lock.notify();
    }

    return toRead;
  }

}
