package ai.swim.structure.processor.writer.writerForm;

import ai.swim.structure.processor.model.ArrayLibraryModel;
import ai.swim.structure.processor.model.CoreTypeModel;
import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.model.InvalidModelException;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.model.ParameterisedTypeModel;
import ai.swim.structure.processor.model.TypeInitializer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import static ai.swim.structure.processor.writer.writerForm.Lookups.ARRAY_WRITER_CLASS;
import static ai.swim.structure.processor.writer.writerForm.Lookups.LIST_WRITER_CLASS;
import static ai.swim.structure.processor.writer.writerForm.Lookups.MAP_WRITER_CLASS;
import static ai.swim.structure.processor.writer.writerForm.Lookups.STD_PACKAGE;

public class WriterTypeInitializer implements TypeInitializer {
  private final ProcessingEnvironment environment;
  private final WriterNameFormatter nameFormatter;

  public WriterTypeInitializer(ProcessingEnvironment environment, WriterNameFormatter nameFormatter) {
    this.environment = environment;
    this.nameFormatter = nameFormatter;
  }

  @Override
  public InitializedType core(CoreTypeModel model) {
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    TypeMirror mirror;
    CodeBlock initializer;

    switch (model.getKind()) {
      case Character:
        mirror = typeUtils.getPrimitiveType(TypeKind.CHAR);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.CHARACTER");
        break;
      case Byte:
        mirror = typeUtils.getPrimitiveType(TypeKind.BYTE);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.BYTE");
        break;
      case Short:
        mirror = typeUtils.getPrimitiveType(TypeKind.SHORT);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.SHORT");
        break;
      case Integer:
        mirror = typeUtils.getPrimitiveType(TypeKind.INT);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.INTEGER");
        break;
      case Long:
        mirror = typeUtils.getPrimitiveType(TypeKind.LONG);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.LONG");
        break;
      case Float:
        mirror = typeUtils.getPrimitiveType(TypeKind.FLOAT);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.FLOAT");
        break;
      case Double:
        mirror = typeUtils.getPrimitiveType(TypeKind.DOUBLE);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.DOUBLE");
        break;
      case Boolean:
        mirror = typeUtils.getPrimitiveType(TypeKind.BOOLEAN);
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.BOOLEAN");
        break;
      case String:
        mirror = elementUtils.getTypeElement(String.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.STRING");
        break;
      case Number:
        mirror = elementUtils.getTypeElement(Number.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.NUMBER");
        break;
      case BigInteger:
        mirror = elementUtils.getTypeElement(BigInteger.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.BIG_INTEGER");
        break;
      case BigDecimal:
        mirror = elementUtils.getTypeElement(BigDecimal.class.getCanonicalName()).asType();
        initializer = CodeBlock.of("ai.swim.structure.writer.std.ScalarWriters.BIG_DECIMAL");
        break;
      default:
        throw new AssertionError("Unhandled primitive type: " + model.getKind());
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
    ClassName className = ClassName.get(STD_PACKAGE, ARRAY_WRITER_CLASS);
    CodeBlock classTy = CodeBlock.of("(Class<$T>) (Class<?>) Object.class", initializedComponentType.getMirror());
    return new InitializedType(
        model.getType(),
        CodeBlock.of(
            "new $T($L, $L)",
            className,
            initializedComponentType.getInitializer(),
            classTy));
  }

  @Override
  public InitializedType untyped(TypeMirror type, boolean inConstructor) {
    Types typeUtils = environment.getTypeUtils();
    TypeMirror erasure = typeUtils.erasure(type);
    return new InitializedType(type, CodeBlock.of("getProxy().lookup((Class<$T>) (Class<?>) $T.class)", type, erasure));
  }

  @Override
  public InitializedType declared(Model model, boolean inConstructor, Model... parameters) {
    ClassName className;
    TypeMirror type = model.getType();

    if (model.isParameterisedType()) {
      ParameterisedTypeModel knownType = (ParameterisedTypeModel) model;
      switch (knownType.getTypeMapping()) {
        case List:
          className = ClassName.get(STD_PACKAGE, LIST_WRITER_CLASS);
          break;
        case Map:
          className = ClassName.get(STD_PACKAGE, MAP_WRITER_CLASS);
          break;
        default:
          throw new AssertionError("Unhandled known type mapping: " + knownType.getTypeMapping());
      }
    } else {
      Element element = model.getElement();
      PackageElement packageElement = model.getDeclaredPackage();
      String recognizerClassName = nameFormatter.writerClassName(element.getSimpleName());
      className = ClassName.bestGuess(String.format("%s.%s", packageElement, recognizerClassName));
    }

    return new InitializedType(type, CodeBlock.of("new $T()", className));
  }

  @Override
  public InitializedType unresolved(Model model, boolean inConstructor) throws InvalidModelException {
    TypeMirror type = model.getType();
    Types typeUtils = environment.getTypeUtils();
    TypeMirror erasure = typeUtils.erasure(type);
    return new InitializedType(type, CodeBlock.of("getProxy().lookup((Class<$T>) (Class<?>) $T.class)", type, erasure));
  }
}
