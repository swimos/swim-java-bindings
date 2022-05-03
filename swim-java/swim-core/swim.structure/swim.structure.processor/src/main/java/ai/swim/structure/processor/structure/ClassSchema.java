package ai.swim.structure.processor.structure;

import ai.swim.structure.processor.ElementMap;
import ai.swim.structure.processor.FieldView;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.structure.accessor.FieldAccessor;
import ai.swim.structure.processor.structure.accessor.MethodAccessor;
import ai.swim.structure.processor.structure.recognizer.ElementRecognizer;
import ai.swim.structure.processor.structure.recognizer.RecognizerModel;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

import static ai.swim.structure.processor.ElementUtils.setterFor;

public class ClassSchema {
  private final String className;
  private final PackageElement packageElement;
  private final ConstructorElement constructor;
  private final List<ElementRecognizer> recognizers;

  private ClassSchema(String className, PackageElement packageElement, ConstructorElement constructor, List<ElementRecognizer> recognizers) {
    this.className = className;
    this.packageElement = packageElement;
    this.constructor = constructor;
    this.recognizers = recognizers;
  }

  public int fieldCount() {
    return this.recognizers.size();
  }

  public PackageElement getPackageElement() {
    return packageElement;
  }

  public List<ElementRecognizer> getRecognizers() {
    return recognizers;
  }

  public static ClassSchema fromMap(ScopedContext context, ElementMap elementMap) {
    List<ElementRecognizer> recognizers = new ArrayList<>();

    for (FieldView field : elementMap.getFields()) {
      RecognizerModel recognizer = RecognizerModel.from(field.getElement(), context);
      if (recognizer == null) {
        continue;
      }

      if (!field.isPublic()) {
        ExecutableElement setter = setterFor(elementMap.getMethods(),field.getName());

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

        recognizers.add(new ElementRecognizer(new MethodAccessor(setter), recognizer, field));
      } else {
        recognizers.add(new ElementRecognizer(new FieldAccessor(field), recognizer, field));
      }
    }

    Elements elementUtils = context.getProcessingContext().getProcessingEnvironment().getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(elementMap.getRoot());



    return new ClassSchema(context.getRoot().getSimpleName().toString(), declaredPackage, elementMap.getConstructor(), recognizers);
  }


  @Override
  public String toString() {
    return "ClassSchema{" + "constructor=" + constructor + ", recognizers=" + recognizers + '}';
  }

  public String className() {
    return this.className;
  }
}
