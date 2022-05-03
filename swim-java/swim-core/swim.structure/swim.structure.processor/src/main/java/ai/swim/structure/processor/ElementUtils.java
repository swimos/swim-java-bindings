package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import java.util.List;

public class ElementUtils {

  public static ExecutableElement setterFor(List<ExecutableElement> methods, Name name) {
    for (ExecutableElement method : methods) {
      String methodName = method.getSimpleName().toString().toLowerCase();
      String fieldName = name.toString().toLowerCase();

      if (methodName.contentEquals("set" + fieldName)) {
        return method;
      }

      AutoForm.Setter setter = method.getAnnotation(AutoForm.Setter.class);

      if (setter != null) {
        if (name.toString().equals(name.toString())) {
          return method;
        }
      }
    }

    return null;
  }
}
