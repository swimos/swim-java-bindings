package ai.swim.structure.processor.schema;

import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.inspect.FieldView;
import ai.swim.structure.processor.inspect.accessor.Accessor;
import ai.swim.structure.processor.recognizer.RecognizerModel;

import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

public class FieldModel {
  private final Accessor accessor;
  private final RecognizerModel recognizer;
  private final FieldView fieldView;

  public FieldModel(Accessor accessor, RecognizerModel recognizer, FieldView fieldView) {
    this.accessor = accessor;
    this.recognizer = recognizer;
    this.fieldView = fieldView;
  }

  @Override
  public String toString() {
    return "FieldModel{" +
        "accessor=" + accessor +
        ", recognizer=" + recognizer +
        ", fieldView=" + fieldView +
        '}';
  }

  public String fieldName() {
    return this.fieldView.getName().toString();
  }

  public String propertyName() {
    return this.fieldView.propertyName();
  }

  public TypeMirror type() {
    TypeMirror type = this.recognizer.type();

    System.out.println("Type: " + type+ ", view: " + fieldView.getElement().asType()+ ", clazz: " + this.recognizer.getClass().getSimpleName());



    if (type == null) {
      return this.fieldView.getElement().asType();
    } else {
      return type;
    }
  }

  public String initializer() {
    return this.recognizer.recognizerInitializer();
  }

  public Accessor getAccessor() {
    return accessor;
  }

  public boolean isOptional() {
    return this.fieldView.isOptional();
  }

  public Object defaultValue() {
    return this.recognizer.defaultValue();
  }

  public FieldKind getFieldKind() {
    return fieldView.getFieldKind();
  }

  public Name getName() {
    return this.fieldView.getName();
  }

  public FieldView getFieldView() {
    return this.fieldView;
  }

  public String transformation() {
    switch (this.fieldView.getFieldKind()) {
      case Body:
        return ".asBodyRecognizer()";
      case Attr:
        return ".asAttrRecognizer()";
      default:
        return "";
    }
  }

}
