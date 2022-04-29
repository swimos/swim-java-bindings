package ai.swim.structure.processor.structure;

import ai.swim.structure.processor.ElementMap;
import ai.swim.structure.processor.FieldView;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.structure.accessor.FieldAccessor;
import ai.swim.structure.processor.structure.accessor.MethodAccessor;
import ai.swim.structure.processor.structure.recognizer.Recognizable;
import ai.swim.structure.processor.structure.recognizer.RecognizerModel;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class ClassSchema {
  private final ConstructorElement constructor;
  private final List<Recognizable> recognizers;

  private ClassSchema(ConstructorElement constructor, List<Recognizable> recognizers) {
    this.constructor = constructor;
    this.recognizers = recognizers;
  }

  public static ClassSchema fromMap(ScopedContext context, ElementMap elementMap) {
    List<Recognizable> recognizers = new ArrayList<>();

    for (FieldView field : elementMap.getFields()) {
      RecognizerModel recognizer = RecognizerModel.from(field.getElement(), context);
      if (recognizer == null) {
        continue;
      }

      if (!field.isPublic()) {
        ExecutableElement setter = elementMap.setterFor(field.getName());

        if (setter == null) {
          context.log(Diagnostic.Kind.ERROR, "Private field: '" + field.getName() + "' has no setter");
          return null;
        }

        List<? extends VariableElement> parameters = setter.getParameters();
        if (parameters.size() != 1) {
          context.log(Diagnostic.Kind.ERROR, "expected a setter for field '" + field.getName() + "' that takes one parameter");
          return null;
        }

        VariableElement variableElement = parameters.get(0);

        if (!variableElement.asType().equals(field.getElement().asType())) {
          context.log(Diagnostic.Kind.ERROR, "setter for field '" + field.getName() + "' accepts an incorrect type");
          return null;
        }

        recognizers.add(new Recognizable(new MethodAccessor(setter), recognizer));
      } else {
        recognizers.add(new Recognizable(new FieldAccessor(field), recognizer));
      }
    }

    return new ClassSchema(elementMap.getConstructor(), recognizers);
  }


  @Override
  public String toString() {
    return "ClassSchema{" + "constructor=" + constructor + ", recognizers=" + recognizers + '}';
  }
}
