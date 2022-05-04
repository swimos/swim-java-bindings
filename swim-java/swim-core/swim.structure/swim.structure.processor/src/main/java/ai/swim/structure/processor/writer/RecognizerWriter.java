package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.structure.ClassSchema;

import java.io.IOException;

public class RecognizerWriter {

  public static final String TYPE_READ_EVENT = "ai.swim.recon.event.ReadEvent";

  public static void writeRecognizer(ClassSchema schema, ScopedContext context) throws IOException {
    BuilderWriter.write(schema, context);
  }


}
