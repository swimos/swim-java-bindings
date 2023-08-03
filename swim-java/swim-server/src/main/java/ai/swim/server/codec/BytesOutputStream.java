package ai.swim.server.codec;

import java.io.IOException;
import java.io.OutputStream;

class BytesOutputStream extends OutputStream {
  private final Bytes inner;

  public BytesOutputStream(Bytes inner) {
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
      super.write(b, off, len);
    }
  }

}
