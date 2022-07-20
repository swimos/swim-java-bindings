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

import ai.swim.bridge.buffer.HeapByteBuffer;
import ai.swim.bridge.channel.exceptions.ChannelClosedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class FfiChannelTest {

  static {
    System.loadLibrary("jvm_sys_tests");
  }

  private static native long readerTask(ByteBuffer buffer, Object lock, byte[] data, Object barrier);

  private static native long writerTask(ByteBuffer buffer, Object lock, byte[] data, int chunkSize, Object barrier);

  private static native long writerClosedTask(ByteBuffer buffer, Object lock, Object barrier);

  private static native long dropReaderTask(ByteBuffer buffer, Object lock, Object barrier);

  private static native long dropWriterTask(ByteBuffer buffer, Object lock, Object barrier);

  private static native void dropRuntime(long ptr);

  void runTest(Function<Object, Long> test) throws InterruptedException {
    Object barrier = new Object();
    Thread notified = new Thread(() -> {
      synchronized (barrier) {
        try {
          barrier.wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });

    notified.start();

    long ptr = test.apply(barrier);
    notified.join();
    dropRuntime(ptr);
  }

  @Test
  @Timeout(10)
  void smallJavaWriter() throws InterruptedException {
    runTest((barrier) -> {
      Object lock = new Object();

      HeapByteBuffer buffer = new HeapByteBuffer(16 + ByteChannel.HEADER_SIZE);
      WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

      byte[] input = new byte[]{1, 2, 3, 4, 5};
      int wrote = writeChannel.write(input);
      assertEquals(wrote, input.length);

      writeChannel.close();
      assertTrue(writeChannel.isClosed());

      return readerTask(buffer.rawBuffer(), lock, input, barrier);
    });
  }

  @Test
  @Timeout(10)
  void smallJavaWriterSpills() throws InterruptedException {
    runTest((barrier) -> {
      Object lock = new Object();

      HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
      WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

      byte[] input = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
      long runtimePtr = readerTask(buffer.rawBuffer(), lock, input, barrier);

      try {
        writeChannel.writeAll(input);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      writeChannel.close();
      assertTrue(writeChannel.isClosed());

      return runtimePtr;
    });
  }

  @Test
  @Timeout(10)
  void largeJavaWriter() throws InterruptedException {
    runTest((barrier) -> {
      Object lock = new Object();

      int channelLen = 4096;
      int dataLen = 1024 * 1024;
      int chunkLen = 1024;

      HeapByteBuffer buffer = new HeapByteBuffer(channelLen + ByteChannel.HEADER_SIZE);
      WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

      byte[] data = new byte[dataLen];
      new Random().nextBytes(data);

      long runtimePtr = readerTask(buffer.rawBuffer(), lock, data, barrier);

      for (int i = 0; i < chunkLen; i++) {
        byte[] chunk = new byte[chunkLen];
        System.arraycopy(data, i * chunkLen, chunk, 0, chunkLen);
        try {
          writeChannel.writeAll(chunk);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }

      writeChannel.close();
      assertTrue(writeChannel.isClosed());

      return runtimePtr;
    });
  }

  @Test
  @Timeout(10)
  void instantlyCloseWriter() throws InterruptedException {
    runTest((barrier) -> {
      Object lock = new Object();

      HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
      WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

      writeChannel.close();

      return readerTask(buffer.rawBuffer(), lock, new byte[0], barrier);
    });
  }

  @Test
  @Timeout(10)
  void smallRead() throws InterruptedException {
    runTest((barrier) -> {
      System.out.println("Starting small read");
      Object lock = new Object();
      HeapByteBuffer buffer = new HeapByteBuffer(16 + ByteChannel.HEADER_SIZE);
      ReadChannel readChannel = new ReadChannel(0, buffer, lock);

      byte[] input = new byte[16];
      new Random().nextBytes(input);

      long ptr = writerTask(buffer.rawBuffer(), lock, input, 16, barrier);

      byte[] readBuf = new byte[16];
      try {
        readChannel.readAll(readBuf);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      assertArrayEquals(input, readBuf);

      assertThrows(ChannelClosedException.class, () -> {
        int i = readChannel.tryRead(new byte[8]);
        System.out.println("smallRead out: " + i);
        assertEquals(0, i);
      });

      readChannel.close();
      assertTrue(readChannel.isClosed());

      return ptr;
    });
  }

  @Test
  @Timeout(10)
  void largeJavaReader() throws InterruptedException {
    runTest((barrier) -> {
      System.out.println("Starting large read");
      Object lock = new Object();

      int channelLen = 4096;
      int dataLen = 1024 * 1024;
      int chunkLen = 1024;

      HeapByteBuffer buffer = new HeapByteBuffer(channelLen + ByteChannel.HEADER_SIZE);
      ReadChannel readChannel = new ReadChannel(0, buffer, lock);

      byte[] input = new byte[dataLen];
      new Random().nextBytes(input);

      long ptr = writerTask(buffer.rawBuffer(), lock, input, chunkLen, barrier);

      byte[] actual = new byte[dataLen];

      for (int i = 0; i < chunkLen; i++) {
        byte[] chunk = new byte[chunkLen];
        try {
          readChannel.readAll(chunk);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        System.arraycopy(chunk, 0, actual, i * chunkLen, chunk.length);
      }

      assertArrayEquals(input, actual);
      assertThrows(ChannelClosedException.class, () -> {
        int i = readChannel.tryRead(new byte[8]);
        System.out.println("Large read: " + i);
        assertEquals(0, i);
      });

      readChannel.close();
      assertTrue(readChannel.isClosed());

      return ptr;
    });
  }

  @Test
  @Timeout(10)
  void instantlyCloseReader() throws InterruptedException {
    runTest((barrier) -> {
      Object lock = new Object();

      HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
      ReadChannel readChannel = new ReadChannel(0, buffer, lock);

      readChannel.close();

      return writerClosedTask(buffer.rawBuffer(), lock, barrier);
    });
  }

  @Test
  @Timeout(10)
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  void dropReader() throws InterruptedException {
    Object lock = new Object();
    Object barrier = new Object();

    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
    WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

    long ptr = dropReaderTask(buffer.rawBuffer(), lock, barrier);

    synchronized (barrier) {
      barrier.wait();
    }

    assertTrue(writeChannel.isClosed());
    dropRuntime(ptr);
  }

  @Test
  @Timeout(10)
  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  void dropWriterTask() throws InterruptedException {
    Object lock = new Object();
    Object barrier = new Object();

    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
    ReadChannel readChannel = new ReadChannel(0, buffer, lock);

    long ptr = dropWriterTask(buffer.rawBuffer(), lock, barrier);

    synchronized (barrier) {
      barrier.wait();
    }

    assertTrue(readChannel.isClosed());
    dropRuntime(ptr);
  }

}
