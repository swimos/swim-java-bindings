package ai.swim.structure.processor.writer;

import java.io.IOException;

public interface Writable {
  void write(Writer writer) throws IOException;
}
