package ai.swim.structure.processor.writer.recognizerForm;

import ai.swim.structure.processor.model.*;
import ai.swim.structure.processor.model.mapping.CoreTypeKind;
import ai.swim.structure.processor.model.mapping.KnownTypeModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.recognizerForm.Lookups.*;

public class RecognizerTypeInitializer implements TypeInitializer {
  private final ProcessingEnvironment environment;
  private final RecognizerNameFormatter nameFormatter;
  private final ModelInspector inspector;

  public RecognizerTypeInitializer(ProcessingEnvironment environment, RecognizerNameFormatter nameFormatter, ModelInspector inspector) {
    this.environment = environment;
    this.nameFormatter = nameFormatter;
    this.inspector = inspector;
  }

  @Override
  public InitializedType core(CoreTypeKind typeKind) {
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    TypeMirror mirror;
    CodeBlock initializer;

    switch (typeKind) {
      case Character:
        mirror = typeUtils.getPrimitiveType(TypeKind.CHAR);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.CHARACTER");
        break;
      case Byte:
        mirror = typeUtils.getPrimitiveType(TypeKind.BYTE);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.BYTE");
        break;
      case Short:
        mirror = typeUtils.getPrimitiveType(TypeKind.SHORT);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.SHORT");
        break;
      case Integer:
        mirror = typeUtils.getPrimitiveType(TypeKind.INT);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.INTEGER");
        break;
      case Long:
        mirror = typeUtils.getPrimitiveType(TypeKind.LONG);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.LONG");
        break;
      case Float:
        mirror = typeUtils.getPrimitiveType(TypeKind.FLOAT);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.FLOAT");
        break;
      case Double:
        mirror = typeUtils.getPrimitiveType(TypeKind.DOUBLE);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.DOUBLE");
        break;
      case Boolean:
        mirror = typeUtils.getPrimitiveType(TypeKind.BOOLEAN);
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.BOOLEAN");
        break;
      case String:
        mirror = elementUtils.getTypeElement(String.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.STRING");
        break;
      case Number:
        mirror = elementUtils.getTypeElement(Number.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.NUMBER");
        break;
      case BigInteger:
        mirror = elementUtils.getTypeElement(BigInteger.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.BIG_INTEGER");
        break;
      case BigDecimal:
        mirror = elementUtils.getTypeElement(BigDecimal.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.recognizer.std.ScalarRecognizer.BIG_DECIMAL");
        break;
      default:
        throw new AssertionError("Unhandled primitive type: " + typeKind);
    }

    if (mirror.getKind().isPrimitive()) {
      TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) mirror);
      mirror = boxedClass.asType();
    }

    return new InitializedType(mirror, initializer);
  }

  @Override
  public InitializedType arrayType(ArrayLibraryModel model, boolean inConstructor) {
    InitializedType initializedComponentType = model.getComponentModel().instantiate(this, inConstructor);
    ClassName className = ClassName.get(COLLECTIONS_PACKAGE, ARRAY_RECOGNIZER_CLASS);
    CodeBlock classTy = CodeBlock.of("(Class<$T>) (Class<?>) Object.class", initializedComponentType.getMirror());
    return new InitializedType(model.getType(), CodeBlock.of("new $T($L, $L)", className, classTy, initializedComponentType.getInitializer()));
  }

  @Override
  public InitializedType untyped(TypeMirror type, boolean inConstructor) {
    if (inConstructor) {
      TypeVariable typeVariable = (TypeVariable) type;
      CodeBlock block = CodeBlock.of("$L.build()", nameFormatter.typeParameterName(typeVariable.toString()));
      return new InitializedType(type, block);
    } else {
      Elements elementUtils = environment.getElementUtils();
      Types typeUtils = environment.getTypeUtils();

      TypeElement typeElement = elementUtils.getTypeElement(UNTYPED_RECOGNIZER);
      DeclaredType declaredType = typeUtils.getDeclaredType(typeElement, type);

      return new InitializedType(type, CodeBlock.of("new $T()", declaredType));
    }
  }

  @Override
  public InitializedType declared(Model model, boolean inConstructor, Model... parameters) {
    TypeMirror type = model.getType();

    CodeBlock.Builder typeParameters = CodeBlock.builder();

    if (parameters != null && parameters.length != 0) {
      boolean first = true;

      for (Model parameter : parameters) {
        if (!first) {
          typeParameters.add(", ");
        } else {
          first = false;
        }

        typeParameters.add(parameter.instantiate(this, inConstructor).getInitializer());
      }
    }

    ClassName className;

    if (model.isKnownType()) {
      KnownTypeModel knownType = (KnownTypeModel) model;
      switch (knownType.getTypeMapping()) {
        case List:
          className = ClassName.get(COLLECTIONS_PACKAGE, LIST_RECOGNIZER_CLASS);
          break;
        case Map:
          className = ClassName.get(STD_PACKAGE, MAP_RECOGNIZER_CLASS);
          break;
        default:
          throw new AssertionError("Unhandled known type mapping: " + knownType.getTypeMapping());
      }
    } else {
      Element element = model.getElement();
      PackageElement packageElement = model.getDeclaredPackage();
      String recognizerClassName = nameFormatter.recognizerClassName(element.getSimpleName().toString());
      className = ClassName.bestGuess(String.format("%s.%s", packageElement, recognizerClassName));
    }

    return new InitializedType(type, CodeBlock.of("new $T($L)", className, typeParameters.build()));
  }

  @Override
  public InitializedType unresolved(Model model, boolean inConstructor) {
    TypeMirror type = model.getType();
    Types typeUtils = environment.getTypeUtils();
    TypeMirror erasure = typeUtils.erasure(type);

    if (type.getKind() == TypeKind.DECLARED) {
      DeclaredType declaredType = (DeclaredType) type;

      String typeParameters = "";

      List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      List<InitializedType> typeArgumentModels = new ArrayList<>(typeArguments.size());

      for (TypeMirror ty : typeArguments) {
        switch (ty.getKind()) {
          case DECLARED:
            typeArgumentModels.add(this.inspector.getOrInspect((TypeElement) ty, environment).instantiate(this, inConstructor));
            break;
          case TYPEVAR:
            typeArgumentModels.add(this.untyped(ty, inConstructor));
          case WILDCARD:
            throw new InvalidModelException("Wildcard type");
          default:
            throw new AssertionError("Unhandled type: " + ty);
        }
      }

      if (typeArgumentModels.size() != 0) {
        typeParameters = typeArgumentModels.stream().map(ty -> String.format("%s.from(() -> %s)", ty.getMirror().toString(), ty.getInitializer())).collect(Collectors.joining(", "));
      }

      typeParameters = typeParameters.isBlank() ? "" : ", " + typeParameters;

//    if (isAbstract) {
//      TypeMirror rootType = context.getRoot().asType();
//      return CodeBlock.of("getProxy().lookup((Class<? extends $T>) (Class<?>) $T.class $L)", rootType, erasure, typeParameters);
//    } else {
      return new InitializedType(type, CodeBlock.of("getProxy().lookup((Class<$T>) (Class<?>) $T.class $L)", declaredType, erasure, typeParameters));
//    }
    } else {
      return new InitializedType(type, CodeBlock.of("getProxy().lookup($T.class)", type, erasure));
    }
  }
}
