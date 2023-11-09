package ai.swim.server.codec;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BytesTest {

  @Test
  void pointers() {
    Bytes bytes = new Bytes();

    assertEquals(0, bytes.remaining());

    bytes.writeInteger(13);
    assertEquals(4, bytes.remaining());

    assertEquals(13, bytes.getInteger());
    assertEquals(0, bytes.remaining());
  }

  @Test
  void intRoundTrip() {
    int prop = 123456789;
    Bytes bytes = new Bytes();
    bytes.writeInteger(prop);

    byte[] data = bytes.getArray();
    bytes = Bytes.fromArray(data, data.length);
    assertEquals(prop, bytes.getInteger());
  }

  @Test
  void longRoundTrip() {
    long prop = 123456789;
    Bytes bytes = new Bytes();
    bytes.writeLong(prop);

    byte[] data = bytes.getArray();
    bytes = Bytes.fromArray(data, data.length);
    assertEquals(prop, bytes.getLong());
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

    Bytes bytes = new Bytes();

    bytes.writeInteger(randomInt);
    bytes.writeLong(randomLong);
    bytes.writeByte(randomByte);
    bytes.writeByteArray(randomBytes);

    byte[] data = bytes.getArray();
    bytes = Bytes.fromArray(data, data.length);

    assertEquals(bytes.getInteger(), randomInt);
    assertEquals(bytes.getLong(), randomLong);
    assertEquals(bytes.getByte(), randomByte);

    byte[] buf = new byte[arrayLen];
    bytes.getByteArray(buf);
    assertArrayEquals(randomBytes, buf);
  }

  @Test
  void splitTo() {
    Bytes bytes = Bytes.fromArray("hello world".getBytes(StandardCharsets.UTF_8));
    Bytes worldBytes = bytes.splitTo(5);

    byte[] helloBuf = new byte[5];
    worldBytes.getByteArray(helloBuf);
    assertEquals("hello", new String(helloBuf, StandardCharsets.UTF_8));

    byte[] worldBuf = new byte[6];
    bytes.getByteArray(worldBuf);
    assertEquals(" world", new String(worldBuf, StandardCharsets.UTF_8));
  }

}