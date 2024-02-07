package ai.swim.codec.data;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper around a {@link ByteWriter} that provides an {@link OutputStream} implementation.
 */
class ByteWriterOutputStream extends OutputStream {
  private final ByteWriter inner;

  public ByteWriterOutputStream(ByteWriter inner) {
    this.inner = inner;
  }

  @Override
  public void write(int b) {
    inner.writeInteger(b);
  }

  @Override
  public void write(byte[] b) {
    inner.writeByteArray(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    if (off == 0) {
      inner.writeByteArray(b, len);
    } else {
      inner.reserve(len);
      super.write(b, off, len);
    }
  }

}
