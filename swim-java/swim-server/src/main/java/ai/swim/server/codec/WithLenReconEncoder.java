package ai.swim.server.codec;

import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.print.StructurePrinter;
import ai.swim.structure.writer.print.strategy.PrintStrategy;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * An encoder that a {@link Writable} into {@link Bytes} and prefixes it by the length of the produced number of bytes.
 */
public class WithLenReconEncoder<V, T extends Writable<V>> implements Encoder<V> {
  private final T writable;

  public WithLenReconEncoder(T writable) {
    this.writable = writable;
  }

  @Override
  public void encode(V target, Bytes dst) {
    int startIdx = dst.remaining();
    dst.writeLong(0);
    int startLen = dst.remaining();

    OutputStreamWriter writer = new OutputStreamWriter(dst.outputStream(), StandardCharsets.UTF_8);
    writable.writeInto(target, new StructurePrinter(writer, PrintStrategy.COMPACT));

    try {
      writer.flush();
    } catch (IOException e) {
      throw new EncoderException(e);
    }

    dst.writeLong(dst.remaining() - startLen, startIdx);
  }
}
