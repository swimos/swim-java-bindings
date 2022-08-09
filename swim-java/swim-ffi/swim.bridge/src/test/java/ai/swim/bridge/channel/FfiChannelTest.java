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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FfiChannelTest {

  static {
    System.loadLibrary("jvm_sys_tests");
  }

  private static native long readerTask(ByteBuffer buffer, Object lock, byte[] data, CountDownLatch latch);

//  private static native long writerTask(ByteBuffer buffer, Object lock, byte[] data, int chunkSize, CountDownLatch latch);

//  private static native long writerClosedTask(ByteBuffer buffer, Object lock, CountDownLatch latch);

  private static native long dropReaderTask(ByteBuffer buffer, Object lock, CountDownLatch latch);

//  private static native long dropWriterTask(ByteBuffer buffer, Object lock, CountDownLatch latch);

  private static native void dropRuntime(long ptr);

  @Test
  @Timeout(60)
  void smallJavaWriter() throws InterruptedException {
    Object lock = new Object();

    HeapByteBuffer buffer = new HeapByteBuffer(16 + ByteChannel.HEADER_SIZE);
    WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

    byte[] input = new byte[] {1, 2, 3, 4, 5};
    int wrote = writeChannel.write(input);
    assertEquals(wrote, input.length);

    writeChannel.close();
    assertTrue(writeChannel.isClosed());

    CountDownLatch latch = new CountDownLatch(1);
    long ptr = readerTask(buffer.rawBuffer(), lock, input, latch);
    latch.await();
    dropRuntime(ptr);
  }

  @Test
  @Timeout(60)
  void smallJavaWriterSpills() throws InterruptedException {
    Object lock = new Object();
    CountDownLatch latch = new CountDownLatch(1);

    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
    WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

    byte[] input = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    long runtimePtr = readerTask(buffer.rawBuffer(), lock, input, latch);

    try {
      writeChannel.writeAll(input);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    writeChannel.close();
    assertTrue(writeChannel.isClosed());

    latch.await();
    dropRuntime(runtimePtr);
  }

  //  @Test
//  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void largeJavaWriter() throws InterruptedException {
    Object lock = new Object();
    CountDownLatch latch = new CountDownLatch(1);

    int channelLen = 4096;
    int dataLen = 1024 * 1024;
    int chunkLen = 1024;

    HeapByteBuffer buffer = new HeapByteBuffer(channelLen + ByteChannel.HEADER_SIZE);
    WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

    byte[] data = new byte[dataLen];
    new Random().nextBytes(data);

    long runtimePtr = readerTask(buffer.rawBuffer(), lock, data, latch);

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

    latch.await();
    dropRuntime(runtimePtr);
  }

  @Test
  @Timeout(60)
  void instantlyCloseWriter() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Object lock = new Object();

    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
    WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

    writeChannel.close();

    long runtimePtr = readerTask(buffer.rawBuffer(), lock, new byte[0], latch);

    latch.await();
    dropRuntime(runtimePtr);
  }

//  @Test
//  @Timeout(60)
//  void smallRead() throws InterruptedException {
//    CountDownLatch latch = new CountDownLatch(1);
//    Object lock = new Object();
//    HeapByteBuffer buffer = new HeapByteBuffer(16 + ByteChannel.HEADER_SIZE);
//    ReadChannel readChannel = new ReadChannel(0, buffer, lock);
//
//    byte[] input = new byte[16];
//    new Random().nextBytes(input);
//
//    long ptr = writerTask(buffer.rawBuffer(), lock, input, 16, latch);
//
//    byte[] readBuf = new byte[16];
//    try {
//      readChannel.readAll(readBuf);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }
//
//    assertArrayEquals(input, readBuf);
//
//    try {
//      assertEquals(0, readChannel.tryRead(new byte[8]));
//    } catch (ChannelClosedException ignored) {
//
//    }
//
//    readChannel.close();
//    assertTrue(readChannel.isClosed());
//
//    latch.await();
//    dropRuntime(ptr);
//  }
//
//  @Test
//  @Timeout(value = 5, unit = TimeUnit.MINUTES)
//  void largeJavaReader() throws InterruptedException {
//    CountDownLatch latch = new CountDownLatch(1);
//    Object lock = new Object();
//
//    int channelLen = 4096;
//    int dataLen = 1024 * 1024;
//    int chunkLen = 1024;
//
//    HeapByteBuffer buffer = new HeapByteBuffer(channelLen + ByteChannel.HEADER_SIZE);
//    ReadChannel readChannel = new ReadChannel(0, buffer, lock);
//
//    byte[] input = new byte[dataLen];
//    new Random().nextBytes(input);
//
//    long ptr = writerTask(buffer.rawBuffer(), lock, input, chunkLen, latch);
//
//    byte[] actual = new byte[dataLen];
//
//    for (int i = 0; i < chunkLen; i++) {
//      byte[] chunk = new byte[chunkLen];
//      try {
//        readChannel.readAll(chunk);
//      } catch (InterruptedException e) {
//        throw new RuntimeException(e);
//      }
//
//      System.arraycopy(chunk, 0, actual, i * chunkLen, chunk.length);
//    }
//
//    assertArrayEquals(input, actual);
//
//    try {
//      assertEquals(0, readChannel.tryRead(new byte[8]));
//    } catch (ChannelClosedException ignored) {
//
//    }
//
//    readChannel.close();
//    assertTrue(readChannel.isClosed());
//
//    latch.await();
//    dropRuntime(ptr);
//  }

//  @Test
//  @Timeout(60)
//  void instantlyCloseReader() throws InterruptedException {
//    CountDownLatch latch = new CountDownLatch(1);
//    Object lock = new Object();
//
//    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
//    ReadChannel readChannel = new ReadChannel(0, buffer, lock);
//
//    readChannel.close();
//
//    long ptr = writerClosedTask(buffer.rawBuffer(), lock, latch);
//    latch.await();
//    dropRuntime(ptr);
//  }

  @Test
  @Timeout(60)
  void dropReader() throws InterruptedException {
    Object lock = new Object();
    CountDownLatch latch = new CountDownLatch(1);

    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
    WriteChannel writeChannel = new WriteChannel(0, buffer, lock);

    long ptr = dropReaderTask(buffer.rawBuffer(), lock, latch);

    latch.await();
    assertTrue(writeChannel.isClosed());
    dropRuntime(ptr);
  }

//  @Test
//  @Timeout(60)
//  void dropWriterTask() throws InterruptedException {
//    Object lock = new Object();
//    CountDownLatch latch = new CountDownLatch(1);
//
//    HeapByteBuffer buffer = new HeapByteBuffer(4 + ByteChannel.HEADER_SIZE);
//    ReadChannel readChannel = new ReadChannel(0, buffer, lock);
//
//    long ptr = dropWriterTask(buffer.rawBuffer(), lock, latch);
//
//    latch.await();
//    assertTrue(readChannel.isClosed());
//    dropRuntime(ptr);
//  }

}
