package ai.swim.structure.processor.inspect;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.recognizer.ClassMap;
import ai.swim.structure.processor.context.ProcessingContext;
import ai.swim.structure.processor.recognizer.RecognizerFactory;
import ai.swim.structure.processor.recognizer.RecognizerModel;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;

public class ElementInspector {

  private ElementInspector() {
  }

  public static ClassMap inspect(Element element, ProcessingContext context) {
    Messager messager = context.getProcessingEnvironment().getMessager();
    ConstructorElement constructor = getConstructor(element, messager);

    if (constructor == null) {
      return null;
    }

    ClassMap classMap = new ClassMap(element, constructor);


    if (!inspectClass(element, classMap)) {
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

  private static boolean inspectGenerics(Element element, ProcessingContext context) {
    DeclaredType declaredType = (DeclaredType) element.asType();

    if (declaredType.getTypeArguments().size() != 0) {
      ProcessingEnvironment environment = context.getProcessingEnvironment();
      environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Class: '" + element + "' has generic type arguments which are not yet supported");
      return false;
    }

    return true;
  }

  private static boolean inspectClass(Element rootElement, ClassMap classMap) {
    for (Element element : rootElement.getEnclosedElements()) {
      switch (element.getKind()) {
        case FIELD:
          classMap.addField(FieldView.from((VariableElement) element));
          break;
        case METHOD:
          classMap.addMethod((ExecutableElement) element);
          break;
      }
    }

    return true;
  }

  private static boolean inspectSuperclasses(Element element, ClassMap classMap, ProcessingContext context) {
    ProcessingEnvironment environment = context.getProcessingEnvironment();
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    Messager messager = environment.getMessager();

    List<? extends TypeMirror> superTypes = typeUtils.directSupertypes(element.asType());

    for (TypeMirror superType : superTypes) {
      if (superType.toString().equals(Object.class.getCanonicalName())) {
        continue;
      }

      TypeElement typeElement = elementUtils.getTypeElement(superType.toString());

      AutoForm autoForm = typeElement.getAnnotation(AutoForm.class);
      if (autoForm == null) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + element + "' extends from '" + superType + "' that is not" + " annotated with @" + AutoForm.class.getSimpleName() + ". Either annotate it or manually implement a form");
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

  private static boolean validateAndMerge(ClassMap classMap, ClassMap superTypeMap, Messager messager) {
    for (FieldView field : classMap.getFields()) {
      if (superTypeMap.getField(field.getName().toString()) != null) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + classMap.getRoot() + "' contains a field (" + field.getName().toString() + ") with the same name as one in its superclass");
        return false;
      }
    }

    classMap.merge(superTypeMap);

    return true;
  }

  private static ConstructorElement getConstructor(Element rootElement, Messager messager) {
    for (Element element : rootElement.getEnclosedElements()) {
      ElementKind elementKind = element.getKind();

      if (elementKind == ElementKind.CONSTRUCTOR) {
        ExecutableElement constructor = (ExecutableElement) element;
        boolean isPublic = false;

        for (Modifier modifier : constructor.getModifiers()) {
          if (modifier == Modifier.PUBLIC) {
            isPublic = true;
            break;
          }
        }

        if (!isPublic) {
          continue;
        }

        if (constructor.getParameters().size() == 0) {
          return new ConstructorElement(constructor);
        }
      }
    }

    messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + rootElement + "' must contain a public constructor with no arguments");
    return null;
  }

}
