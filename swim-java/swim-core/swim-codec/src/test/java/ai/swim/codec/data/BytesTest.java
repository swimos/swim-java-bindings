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

package ai.swim.codec.data;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytesTest {

  /**
   * Tests that the splitTo operation returns the right halves after performing read operations.
   */
  @Test
  void splitTo() {
    ByteReader reader = ByteReader.fromArray(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

    assertEquals(0, reader.getByte());
    assertEquals(1, reader.getByte());
    assertEquals(2, reader.getByte());

    assertEquals(7, reader.remaining());

    ByteReader start = reader.splitTo(4);

    assertArrayEquals(new byte[] {3, 4, 5, 6}, start.peekArray());
    assertArrayEquals(new byte[] {7, 8, 9}, reader.peekArray());

    start.advance(4);
    assertEquals(0, start.remaining());

    reader.advance(3);
    assertEquals(0, reader.remaining());

    assertThrows(BufferOverflowException.class, () -> start.advance(10));
    assertThrows(BufferOverflowException.class, () -> reader.advance(10));
  }

  /**
   * Asserts that the right halves are returned when no data has been read.
   */
  @Test
  void splitTo2() {
    ByteReader reader = ByteReader.fromArray(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

    ByteReader start = reader.splitTo(4);

    assertArrayEquals(new byte[] {0, 1, 2, 3}, start.getArray());
    assertArrayEquals(new byte[] {4, 5, 6, 7, 8, 9}, reader.getArray());
  }

  @Test
  void pointers() {
    ByteWriter writer = new ByteWriter();
    writer.writeInteger(13);

    ByteReader reader = writer.reader();

    assertEquals(4, reader.remaining());
    assertEquals(13, reader.getInteger());
    assertEquals(0, reader.remaining());

    // Asserts that a subsequent write to the underlying array is not visible to the reader.
    writer.writeInteger(26);
    assertEquals(0, reader.remaining());

    // Now it should be visible
    reader = writer.reader();
    assertEquals(13, reader.getInteger());
    assertEquals(26, reader.getInteger());
  }

  @Test
  void intRoundTrip() {
    int prop = 123456789;
    ByteWriter writer = new ByteWriter();
    writer.writeInteger(prop);

    ByteReader reader = writer.reader();
    assertEquals(prop, reader.getInteger());
  }

  @Test
  void longRoundTrip() {
    long prop = 123456789;
    ByteWriter writer = new ByteWriter();
    writer.writeLong(prop);

    ByteReader reader = writer.reader();
    assertEquals(prop, reader.getLong());
  }

  @Test
  void contiguousTypes() {
    Random random = new Random();
    int randomInt = random.nextInt();
    long randomLong = random.nextLong();
    byte randomByte = (byte) random.nextInt();
    int arrayLen = 64;
    byte[] randomBytes = new byte[arrayLen];
    random.nextBytes(randomBytes);

    ByteWriter writer = new ByteWriter();

    writer.writeInteger(randomInt);
    writer.writeLong(randomLong);
    writer.writeByte(randomByte);
    writer.writeByteArray(randomBytes);

    ByteReader reader = writer.reader();

    assertEquals(reader.getInteger(), randomInt);
    assertEquals(reader.getLong(), randomLong);
    assertEquals(reader.getByte(), randomByte);

    byte[] buf = new byte[arrayLen];
    reader.getByteArray(buf);
    assertArrayEquals(randomBytes, buf);
  }

  @Test
  void unsplitDifferentBuffers() {
    ByteWriter aWriter = new ByteWriter();
    aWriter.writeInteger(10);

    ByteWriter bWriter = new ByteWriter();
    bWriter.writeInteger(20);

    ByteReader aReader = aWriter.reader();
    ByteReader bReader = bWriter.reader();

    aReader.unsplit(bReader);
    assertTrue(bReader.isEmpty());

    assertEquals(10, aReader.getInteger());
    assertEquals(20, aReader.getInteger());
  }

  @Test
  void unsplitSameBuffer() {
    ByteWriter writer = new ByteWriter();

    writer.writeInteger(10);
    writer.writeInteger(20);

    ByteReader aReader = writer.reader();
    ByteReader bReader = aReader.splitTo(4);

    assertEquals(20, aReader.peekInteger());
    assertEquals(10, bReader.peekInteger());

    aReader.unsplit(bReader);
    assertTrue(bReader.isEmpty());

    assertEquals(20, aReader.getInteger());
    assertTrue(aReader.isEmpty());
    assertTrue(bReader.isEmpty());
  }

  @Test
  void rollingHashcodes() {
    ByteWriter left = new ByteWriter();
    ByteWriter right = new ByteWriter();

    left.writeInteger(1);
    right.writeInteger(1);
    assertEquals(left.getElementHashcode(), right.getElementHashcode());

    left.writeInteger(2);
    right.writeInteger(2);
    assertEquals(left.getElementHashcode(), right.getElementHashcode());

    left.writeInteger(3);
    right.writeInteger(3);
    assertEquals(left.getElementHashcode(), right.getElementHashcode());
  }

}