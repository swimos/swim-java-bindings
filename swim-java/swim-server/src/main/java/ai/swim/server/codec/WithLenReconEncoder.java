package ai.swim.server.codec;

import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.print.StructurePrinter;
import ai.swim.structure.writer.print.strategy.PrintStrategy;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class WithLenReconEncoder<V, T extends Writable<V>> implements Encoder<V> {
  private final T writable;

  public WithLenReconEncoder(T writable) {
    this.writable = writable;
  }

  @Override
  public void encode(V target, Bytes dst) {
    int startIdx = dst.remaining();
    dst.writeInteger(0);

    OutputStreamWriter writer = new OutputStreamWriter(dst.outputStream(), StandardCharsets.UTF_8);
    writable.writeInto(target, new StructurePrinter(writer, PrintStrategy.COMPACT));

    try {
      writer.flush();
    } catch (IOException e) {
      throw new EncoderException(e);
    }

    dst.writeInteger(dst.remaining() - startIdx, startIdx);
  }
}
