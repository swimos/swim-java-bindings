package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.inspect.FieldView;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.ConstructorElement;
import ai.swim.structure.processor.inspect.accessor.FieldAccessor;
import ai.swim.structure.processor.inspect.accessor.MethodAccessor;
import ai.swim.structure.processor.recognizer.ClassMap;
import ai.swim.structure.processor.recognizer.ElementRecognizer;
import ai.swim.structure.processor.recognizer.RecognizerModel;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
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

  public PackageElement getPackageElement() {
    return packageElement;
  }

  public List<ElementRecognizer> getRecognizers() {
    return recognizers;
  }

  public static ClassSchema fromMap(ScopedContext context, ClassMap classMap) {
    List<ElementRecognizer> recognizers = new ArrayList<>();
    Types typeUtils = context.getProcessingContext().getProcessingEnvironment().getTypeUtils();

    for (FieldView field : classMap.getFields()) {
      if (field.isIgnored()) {
        continue;
      }

      RecognizerModel recognizer = RecognizerModel.from(field.getElement(), context);
      if (recognizer == null) {
        if (field.isOptional()) {
          // todo wrap field in an OptionalRecognizer instead of skipping it
          continue;
        }

//        context.log(Diagnostic.Kind.ERROR, "Failed to find a recognizer for field: '" + field.getName() + "'");
//        return null;
        continue;
      }

      if (field.isPublic()) {
        recognizers.add(new ElementRecognizer(new FieldAccessor(field), recognizer, field));
      } else {
        ExecutableElement setter = setterFor(classMap.getMethods(), field.getName());

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

        if (!typeUtils.isSameType(variableElement.asType(), field.getElement().asType())){
          String cause = String.format("Expected type: '%s', found: '%s'", variableElement.asType(), field.getElement().asType());
          context.log(Diagnostic.Kind.ERROR, "setter for field '" + field.getName() + "' accepts an incorrect type. Cause: "+cause);
          return null;
        }

        recognizers.add(new ElementRecognizer(new MethodAccessor(setter), recognizer, field));
      }
    }

    Elements elementUtils = context.getProcessingContext().getProcessingEnvironment().getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(classMap.getRoot());

    return new ClassSchema(context.getRoot().getSimpleName().toString(), declaredPackage, classMap.getConstructor(), recognizers);
  }

  @Override
  public String toString() {
    return "ClassSchema{" + "constructor=" + constructor + ", recognizers=" + recognizers + '}';
  }

  public String className() {
    return this.className;
  }
}
