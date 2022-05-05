package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.inspect.FieldView;
import ai.swim.structure.processor.inspect.accessor.Accessor;

import javax.lang.model.type.TypeMirror;

public class ElementRecognizer {
  private final Accessor accessor;
  private final RecognizerModel recognizer;
  private final FieldView fieldView;

  public ElementRecognizer(Accessor accessor, RecognizerModel recognizer, FieldView fieldView) {
    this.accessor = accessor;
    this.recognizer = recognizer;
    this.fieldView = fieldView;
  }

  @Override
  public String toString() {
    return "ElementRecognizer{" +
        "accessor=" + accessor +
        ", recognizer=" + recognizer +
        ", fieldView=" + fieldView +
        '}';
  }

  public String fieldName() {
    return this.fieldView.getName().toString();
  }

  public TypeMirror type() {
    return this.fieldView.getElement().asType();
  }

  public String initializer() {
    return this.recognizer.initializer();
  }

  public Accessor getAccessor() {
    return accessor;
  }
}
