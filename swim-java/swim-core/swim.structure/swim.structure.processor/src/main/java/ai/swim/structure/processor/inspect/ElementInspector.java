package ai.swim.structure.processor.inspect;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.context.ScopedMessager;
import ai.swim.structure.processor.inspect.accessor.FieldAccessor;
import ai.swim.structure.processor.inspect.accessor.MethodAccessor;
import ai.swim.structure.processor.recognizer.RecognizerFactory;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import ai.swim.structure.processor.schema.FieldModel;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

import static ai.swim.structure.processor.ElementUtils.getNoArgConstructor;
import static ai.swim.structure.processor.ElementUtils.setterFor;

public class ElementInspector {

  private ElementInspector() {

  }

  public static ClassMap inspect(Element element, ScopedContext context) {
    ProcessingEnvironment env = context.getProcessingEnvironment();
    ConstructorElement constructor = getConstructor(element, context.getMessager());

    if (constructor == null) {
      return null;
    }

    Elements elementUtils = env.getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(element);

    ClassMap classMap = new ClassMap(element, constructor, declaredPackage);

    if (!inspectClass(element, classMap, context)) {
      return null;
    }

    if (!inspectSuperclasses(element, classMap, context)) {
      return null;
    }

    if (!inspectGenerics(element, context)) {
      return null;
    }

    return classMap;
  }

  private static boolean inspectGenerics(Element element, ScopedContext context) {
    DeclaredType declaredType = (DeclaredType) element.asType();

    if (declaredType.getTypeArguments().size() != 0) {
      ScopedMessager messager = context.getMessager();
      messager.error("Class: has generic type arguments which are not supported by the form annotation processor");
      return false;
    }

    return true;
  }

  private static boolean inspectClass(Element rootElement, ClassMap classMap, ScopedContext ctx) {
    List<FieldView> fieldViews = new ArrayList<>();

    for (Element element : rootElement.getEnclosedElements()) {
      switch (element.getKind()) {
        case FIELD:
          fieldViews.add(FieldView.from((VariableElement) element, getFieldKind(element)));
          break;
        case METHOD:
          classMap.addMethod((ExecutableElement) element);
          break;
      }
    }

    Manifest manifest = new Manifest();

    for (FieldView field : fieldViews) {
      if (field.isIgnored()) {
        continue;
      }

      if (!manifest.validate(field, ctx.getMessager())) {
        return false;
      }

      RecognizerModel recognizerModel = RecognizerModel.from(field.getElement(), ctx);

      if (!field.isPublic()) {
        ExecutableElement setter = setterFor(classMap.getMethods(), field.getName());

        if (!validateSetter(setter, field.getName(), field.getElement().asType(), ctx)) {
          return false;
        }

        classMap.addField(new FieldModel(new MethodAccessor(setter), recognizerModel, field));
      } else {
        classMap.addField(new FieldModel(new FieldAccessor(field.getName().toString()), recognizerModel, field));
      }
    }

    return true;
  }

  private static boolean validateSetter(ExecutableElement setter, Name name, TypeMirror expectedType, ScopedContext ctx) {
    ScopedMessager messager = ctx.getMessager();
    Types typeUtils = ctx.getProcessingEnvironment().getTypeUtils();

    if (setter == null) {
      messager.error("Private field: '" + name + "' has no setter");
      return false;
    }

    List<? extends VariableElement> parameters = setter.getParameters();
    if (parameters.size() != 1) {
      messager.error("expected a setter for field '" + name + "' that takes one parameter");
      return false;
    }

    VariableElement variableElement = parameters.get(0);

    if (!typeUtils.isSameType(variableElement.asType(), expectedType)) {
      String cause = String.format("Expected type: '%s', found: '%s'", variableElement.asType(), expectedType);
      messager.error("setter for field '" + name + "' accepts an incorrect type. Cause: " + cause);
      return false;
    }

    return true;
  }

  private static FieldKind getFieldKind(Element element) {
    AutoForm.Kind kind = element.getAnnotation(AutoForm.Kind.class);
    if (kind == null) {
      return FieldKind.Slot;
    } else {
      return kind.value();
    }
  }

  private static boolean inspectSuperclasses(Element element, ClassMap classMap, ScopedContext context) {
    ProcessingEnvironment environment = context.getProcessingEnvironment();
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    ScopedMessager messager = context.getMessager();

    List<? extends TypeMirror> superTypes = typeUtils.directSupertypes(element.asType());

    for (TypeMirror superType : superTypes) {
      if (superType.toString().equals(Object.class.getCanonicalName())) {
        continue;
      }

      TypeElement typeElement = elementUtils.getTypeElement(superType.toString());

      AutoForm autoForm = typeElement.getAnnotation(AutoForm.class);
      if (autoForm == null) {
        messager.error("Class extends from '" + superType + "' that is not" + " annotated with @" + AutoForm.class.getSimpleName() + ". Either annotate it or manually implement a form");
        return false;
      }

      RecognizerFactory factory = context.getFactory();
      RecognizerModel superTypeModel = factory.getOrInspect(typeElement, context);

      if (superTypeModel == null) {
        return false;
      }

      if (superTypeModel instanceof ClassMap) {
        if (!validateAndMerge(classMap, (ClassMap) superTypeModel, messager)) {
          return false;
        }
      }
    }

    return true;
  }

  private static boolean validateAndMerge(ClassMap classMap, ClassMap superTypeMap, ScopedMessager messager) {
    for (FieldView field : classMap.getFieldViews()) {
      FieldView parentField = superTypeMap.getFieldView(field.getName().toString());

      if (parentField != null && !parentField.isIgnored()) {
        messager.error("Class contains a field (" + field.getName().toString() + ") with the same name as one in its superclass");
        return false;
      }
    }

    classMap.merge(superTypeMap);

    return true;
  }

  private static ConstructorElement getConstructor(Element rootElement, ScopedMessager messager) {
    ExecutableElement constructor = getNoArgConstructor(rootElement);

    if (constructor == null) {
      messager.error("Class must contain a public constructor with no arguments");
      return null;
    }

    return new ConstructorElement(constructor);
  }

}
