package ai.swim.structure.processor;

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
}
