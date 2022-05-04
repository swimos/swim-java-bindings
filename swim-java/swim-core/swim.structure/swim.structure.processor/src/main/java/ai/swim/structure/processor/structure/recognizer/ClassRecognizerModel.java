package ai.swim.structure.processor.structure.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public abstract class ClassRecognizerModel extends RecognizerModel {
  public static RecognizerModel collection(VariableElement element, ScopedContext context) {
    context.log(Diagnostic.Kind.ERROR, "Collection.class model unimplemented");
    return null;
  }

  public static RecognizerModel map(VariableElement element, ScopedContext context) {
    context.log(Diagnostic.Kind.ERROR, "Map.class model unimplemented");
    return null;
  }

}
