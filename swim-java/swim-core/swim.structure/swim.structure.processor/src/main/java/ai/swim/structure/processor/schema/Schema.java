package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.models.ClassMap;
import ai.swim.structure.processor.recognizer.models.InterfaceMap;
import ai.swim.structure.processor.recognizer.models.RecognizerModel;

import javax.lang.model.element.Element;
import java.io.IOException;

public interface Schema {

  static Schema from(RecognizerModel recognizer) {
    if (recognizer instanceof ClassMap) {
      return ClassSchema.fromMap((ClassMap) recognizer);
    } else if (recognizer instanceof InterfaceMap) {
      return InterfaceSchema.fromMap((InterfaceMap) recognizer);
    } else {
      throw new RuntimeException("Unimplemented schema type: " + recognizer);
    }
  }

  Element root();

  void write(ScopedContext scopedContext) throws IOException;

}
