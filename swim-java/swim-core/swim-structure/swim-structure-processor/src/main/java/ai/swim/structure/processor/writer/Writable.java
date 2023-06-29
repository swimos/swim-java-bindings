package ai.swim.structure.processor.writer;

import java.io.IOException;

/**
 * Interface for structural models that are written as recognizers and/or writers.
 */
public interface Writable {
  /**
   * Write this structural writer using {@code Writer}.
   */
  void write(Writer writer) throws IOException;
}
