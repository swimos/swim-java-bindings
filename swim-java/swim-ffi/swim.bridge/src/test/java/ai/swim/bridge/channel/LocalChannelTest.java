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
import ai.swim.bridge.buffer.HeapByteBuffer;
import ai.swim.bridge.channel.exceptions.ChannelClosedException;
import ai.swim.bridge.channel.exceptions.InsufficientCapacityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class LocalChannelTest {

  private static class TestReadChannel extends ReadChannel {
    TestReadChannel(long ptr, Buffer buffer, Object lock) {
      super(ptr, buffer, lock);
    }

//    @Override
//    protected void wake() {
//      // no-op to prevent segfault
//    }
//
//    @Override
//    protected void free() {
//      // no-op to prevent segfault
//    }
  }

  private static class TestWriteChannel extends WriteChannel {
    TestWriteChannel(long ptr, Buffer buffer, Object lock) {
      super(ptr, buffer, lock);
    }

//    @Override
//    protected void wake() {
//      // no-op to prevent segfault
//    }
//
//    @Override
//    protected void free() {
//      // no-op to prevent segfault
//    }
  }

  private static class TestChannel {
    ReadChannel readChannel;
    WriteChannel writeChannel;

    private TestChannel(ReadChannel readChannel, WriteChannel writeChannel) {
      this.readChannel = readChannel;
      this.writeChannel = writeChannel;
    }

    public static TestChannel newChannel(int capacity) {
      Buffer buffer = new HeapByteBuffer(capacity + ByteChannel.HEADER_SIZE);
      Object lock = new Object();
      ReadChannel readChannel = new TestReadChannel(0, buffer, lock);
      WriteChannel writeChannel = new TestWriteChannel(0, buffer, lock);

      return new TestChannel(readChannel, writeChannel);
    }
  }

  /**
   * Write in to the buffer and read back out using a shared buffer and no FFI calls.
   */
  @Test
  void fullCircle() {
    TestChannel testChannel = TestChannel.newChannel(128);
    ReadChannel readChannel = testChannel.readChannel;
    WriteChannel writeChannel = testChannel.writeChannel;

    byte[] input = new byte[]{1, 2, 3, 4, 5};
    int wrote = writeChannel.write(input);
    assertEquals(input.length, wrote);

    byte[] readBuf = new byte[input.length];
    int read = readChannel.tryRead(readBuf);

    assertEquals(input.length, read);
    assertArrayEquals(readBuf, input);

    read = readChannel.tryRead(readBuf);
    assertEquals(0, read);
  }

  @Test
  void readCapacity0() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> Channels.readChannel(0));
    assertEquals("Cannot create a channel with a capacity of less than 2", exception.getMessage());
  }

  @Test
  void writeCapacity0() {
    RuntimeException exception = assertThrows(RuntimeException.class, () -> Channels.writeChannel(0));
    assertEquals("Cannot create a channel with a capacity of less than 2", exception.getMessage());
  }

  @Test
  void closeReaderEmpty() {
    TestChannel testChannel = TestChannel.newChannel(128);
    ReadChannel readChannel = testChannel.readChannel;
    WriteChannel writeChannel = testChannel.writeChannel;

    readChannel.close();

    ChannelClosedException exception = assertThrows(ChannelClosedException.class, () -> writeChannel.write(new byte[1]));
    assertEquals("Channel closed", exception.getMessage());
  }

  @Test
  void closeWriterEmpty() {
    TestChannel testChannel = TestChannel.newChannel(128);
    ReadChannel readChannel = testChannel.readChannel;
    WriteChannel writeChannel = testChannel.writeChannel;

    writeChannel.close();

    ChannelClosedException exception = assertThrows(ChannelClosedException.class, () -> readChannel.tryRead(new byte[1]));
    assertEquals("Channel closed", exception.getMessage());
  }

  @Test
  void closeFlushes() {
    TestChannel testChannel = TestChannel.newChannel(128);
    ReadChannel readChannel = testChannel.readChannel;
    WriteChannel writeChannel = testChannel.writeChannel;

    byte[] in = new byte[]{1, 2, 3, 4, 5};
    int wrote = writeChannel.write(in);
    assertEquals(wrote, in.length);

    readChannel.close();

    byte[] buf = new byte[in.length];
    int read = readChannel.tryRead(buf);
    assertEquals(read, in.length);
    assertArrayEquals(in, buf);

    ChannelClosedException exception = assertThrows(ChannelClosedException.class, () -> readChannel.tryRead(new byte[1]));
    assertEquals("Channel closed", exception.getMessage());
  }

  @Test
  void emptyRead() {
    TestChannel testChannel = TestChannel.newChannel(128);
    ReadChannel readChannel = testChannel.readChannel;

    int read = readChannel.tryRead(new byte[0]);
    assertEquals(0, read);

    RuntimeException exception = assertThrows(NullPointerException.class, () -> readChannel.tryRead(null));
    assertEquals("Provided buffer is null", exception.getMessage());
  }

  @Test
  @Timeout(value = 10)
  void bulkSend() throws InterruptedException, ExecutionException {
    int dataLength = 4096;
    int capacity = 128;

    TestChannel testChannel = TestChannel.newChannel(capacity);
    ReadChannel readChannel = testChannel.readChannel;
    WriteChannel writeChannel = testChannel.writeChannel;

    Random random = new Random();
    byte[] in = new byte[dataLength];
    random.nextBytes(in);

    Runnable writeTask = () -> {
      byte[] buf = Arrays.copyOf(in, in.length);
      int total = 0;
      while (true) {
        try {
          int wrote = writeChannel.write(buf);
          total += wrote;
          if (wrote == 0) {
            break;
          } else {
            buf = Arrays.copyOfRange(buf, wrote, buf.length);
          }
        } catch (InsufficientCapacityException e) {
          sleep();
        }
      }

      assertEquals(in.length, total);
      writeChannel.close();
      assertTrue(writeChannel.isClosed());
    };

    Runnable readTask = () -> {
      int idx = 0;
      byte[] out = new byte[dataLength];
      while (idx != in.length) {
        byte[] buf = new byte[8];
        int read;

        try {
          read = readChannel.tryRead(buf);
          if (read == 0) {
            sleep();
            continue;
          }
        } catch (ChannelClosedException e) {
          break;
        }

        System.arraycopy(buf, 0, out, idx, read);
        idx += read;
      }

      assertArrayEquals(in, out);
      readChannel.close();
      assertTrue(readChannel.isClosed());
    };

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    Future<?> writeFuture = executorService.submit(writeTask);
    Future<?> readFuture = executorService.submit(readTask);

    writeFuture.get();
    readFuture.get();
  }

  private static void sleep() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Test
  void testReadWriteAll() throws ExecutionException, InterruptedException {
    int dataLength = 4096;
    int capacity = 128;

    TestChannel testChannel = TestChannel.newChannel(capacity);
    ReadChannel readChannel = testChannel.readChannel;
    WriteChannel writeChannel = testChannel.writeChannel;

    Random random = new Random();
    byte[] in = new byte[dataLength];
    random.nextBytes(in);

    Runnable writeTask = () -> {
      try {
        writeChannel.writeAll(in);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      writeChannel.close();
      assertTrue(writeChannel.isClosed());
    };

    Runnable readTask = () -> {
      byte[] out = new byte[dataLength];
      try {
        readChannel.readAll(out);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

      assertArrayEquals(in, out);
      assertTrue(readChannel.isClosed());
    };

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    Future<?> writeFuture = executorService.submit(writeTask);
    Future<?> readFuture = executorService.submit(readTask);

    writeFuture.get();
    readFuture.get();
  }
}