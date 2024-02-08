/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server.buffer;

import org.junit.jupiter.api.Test;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JniChannelTest {

  @Test
  void simpleReadWrite() {
    JniChannel channel = JniChannel.fromByteBuffer(ByteBuffer.allocate(128));
    ByteWriter writer = channel.writer();

    writer.writeInteger(10);
    writer.writeInteger(20);
    writer.writeInteger(30);

    channel.free(12);

    ByteReader reader = channel.reader();

    assertEquals(12, reader.remaining());

    assertEquals(10, reader.getInt());
    assertEquals(20, reader.getInt());
    assertEquals(30, reader.getInt());

    assertThrows(BufferUnderflowException.class, reader::getInt);
  }

  @Test
  void multiple() {
    JniChannel channel = JniChannel.fromByteBuffer(ByteBuffer.allocate(128));
    ByteWriter writer = channel.writer();

    writer.writeInteger(10);
    writer.writeInteger(20);
    writer.writeInteger(30);
    writer.writeInteger(40);
    writer.writeInteger(50);

    channel.free(12);

    ByteReader reader = channel.reader();

    assertEquals(12, reader.remaining());

    assertEquals(10, reader.getInt());
    assertEquals(20, reader.getInt());
    assertEquals(30, reader.getInt());

    assertThrows(BufferUnderflowException.class, reader::getInt);

    channel.free(8);

    assertEquals(40, reader.getInt());
    assertEquals(50, reader.getInt());

    assertThrows(BufferUnderflowException.class, reader::getInt);
  }

  @Test
  void wraps() {
    JniChannel channel = JniChannel.fromByteBuffer(ByteBuffer.allocate(20));
    ByteWriter writer = channel.writer();

    writer.writeInteger(10);
    writer.writeInteger(20);
    writer.writeInteger(30);
    writer.writeInteger(40);
    writer.writeInteger(50);

    channel.free(20);

    ByteReader reader = channel.reader();

    assertEquals(10, reader.getInt());
    assertEquals(20, reader.getInt());
    assertEquals(30, reader.getInt());
    assertEquals(40, reader.getInt());

    assertEquals(50, reader.peekInt());

    // simulate Rust looping and writing at the start

    writer.writeInteger(60, 0);
    writer.writeInteger(70, 4);

    channel.free(8);

    assertEquals(50, reader.getInt());
    assertEquals(60, reader.getInt());
    assertEquals(70, reader.getInt());

    assertThrows(BufferUnderflowException.class, reader::getInt);
  }

  @Test
  void unavailable() {
    JniChannel channel = JniChannel.fromByteBuffer(ByteBuffer.allocate(4));
    ByteWriter writer = channel.writer();

    writer.writeInteger(10);
    channel.free(4);

    ByteReader reader = channel.reader();
    assertEquals(4, reader.remaining());

    assertThrows(BufferUnderflowException.class, reader::getLong);
    assertThrows(BufferUnderflowException.class, reader::peekLong);

    assertEquals(10, reader.getInt());

    assertThrows(BufferUnderflowException.class, reader::getLong);
    assertThrows(BufferUnderflowException.class, reader::peekLong);
    assertThrows(BufferUnderflowException.class, reader::getInt);
    assertThrows(BufferUnderflowException.class, reader::peekInt);
    assertThrows(BufferUnderflowException.class, reader::getByte);
    assertThrows(BufferUnderflowException.class, reader::peekByte);
    assertThrows(BufferUnderflowException.class, () -> reader.peekByte(0));
    assertThrows(IllegalArgumentException.class, () -> reader.peekByte(-1));
  }

}