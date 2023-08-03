package ai.swim.server.codec;

import ai.swim.structure.Recon;
import ai.swim.structure.annotations.AutoForm;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WithLenReconEncoderTest {

  @Test
  void testEncode() {
    String string = Recon.toStringCompact(new Prop(1, 2));
    int len = string.length();

    WithLenReconEncoder<Prop, PropWriter> encoder = new WithLenReconEncoder<>(new PropWriter());
    Bytes bytes = new Bytes();
    encoder.encode(new Prop(1, 2), bytes);

    byte[] expected = new byte[bytes.capacity()];
    expected[3] = (byte) (len + Size.INT);
    byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
    System.arraycopy(stringBytes, 0, expected, 4, stringBytes.length);

    assertArrayEquals(expected, bytes.getArray());
  }

  @AutoForm
  public static class Prop {
    public int a;
    public int b;

    public Prop() {

    }

    public Prop(int a, int b) {
      this.a = a;
      this.b = b;
    }
  }

}