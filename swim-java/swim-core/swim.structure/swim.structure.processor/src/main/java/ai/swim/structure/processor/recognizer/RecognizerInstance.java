package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public class RecognizerInstance extends StructuralRecognizer {

  private final String type;

  public RecognizerInstance(String type) {
    this.type = type;
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new %s()", type);
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return null;
  }

  @Override
  public RecognizerModel retyped(ScopedContext context) {
    return this;
  }

}
