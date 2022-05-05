package ai.swim.structure.processor.inspect;

import ai.swim.structure.annotations.AutoForm;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;

public class FieldView {
  private final VariableElement element;

  private FieldView(VariableElement element) {
    this.element = element;
  }

  public static FieldView from(VariableElement element) {
    return new FieldView(element);
  }

  public Name getName() {
    return this.element.getSimpleName();
  }

  public boolean isPublic() {
    for (Modifier modifier : this.element.getModifiers()) {
      switch (modifier) {
        case PUBLIC:
          return true;
        case PROTECTED:
        case PRIVATE:
          return false;
      }
    }

    return false;
  }

  public VariableElement getElement() {
    return element;
  }

  public boolean isOptional() {
    return this.element.getAnnotation(AutoForm.Optional.class) != null;
  }

  public boolean isIgnored() {
    return this.element.getAnnotation(AutoForm.Ignore.class) != null;
  }

  @Override
  public String toString() {
    return "FieldView{" +
        "element=" + element +
        '}';
  }
}
