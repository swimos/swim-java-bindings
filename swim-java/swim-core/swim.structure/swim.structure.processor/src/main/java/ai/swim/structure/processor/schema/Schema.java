package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.ClassMap;
import ai.swim.structure.processor.recognizer.InterfaceMap;
import ai.swim.structure.processor.recognizer.StructuralRecognizer;

import javax.lang.model.element.Element;
import java.io.IOException;

public interface Schema {

  static Schema from(StructuralRecognizer recognizer) {
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
