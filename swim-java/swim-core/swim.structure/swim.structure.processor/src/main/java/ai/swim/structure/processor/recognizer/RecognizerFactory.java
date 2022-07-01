package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.ElementInspector;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RecognizerFactory {
  private final HashMap<String, RecognizerModel> recognizers;

  private RecognizerFactory(HashMap<String, RecognizerModel> recognizers) {
    this.recognizers = recognizers;
  }

  /**
   * Initialises standard library types.
   */
  public static RecognizerFactory initFrom(ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    HashMap<String, RecognizerModel> recognizers = new HashMap<>();

    // init core types
    RecognizerInstance.Resolver primitiveResolver = RecognizerInstance.resolver("ai.swim.structure.recognizer.std.ScalarRecognizer");
    recognizers.put(_getOrThrowType(elementUtils, Byte.class), primitiveResolver.resolve(processingEnvironment, "BYTE"));
    recognizers.put(_getOrThrowType(elementUtils, Short.class), primitiveResolver.resolve(processingEnvironment, "SHORT"));
    recognizers.put(_getOrThrowType(elementUtils, Integer.class), primitiveResolver.resolve(processingEnvironment, "INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, Long.class), primitiveResolver.resolve(processingEnvironment, "LONG"));
    recognizers.put(_getOrThrowType(elementUtils, Float.class), primitiveResolver.resolve(processingEnvironment, "FLOAT"));
    recognizers.put(_getOrThrowType(elementUtils, Boolean.class), primitiveResolver.resolve(processingEnvironment, "BOOLEAN"));
    recognizers.put(_getOrThrowType(elementUtils, String.class), primitiveResolver.resolve(processingEnvironment, "STRING"));
    recognizers.put(_getOrThrowType(elementUtils, Character.class), primitiveResolver.resolve(processingEnvironment, "CHARACTER"));
    recognizers.put(_getOrThrowType(elementUtils, BigInteger.class), primitiveResolver.resolve(processingEnvironment, "BIG_INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, BigDecimal.class), primitiveResolver.resolve(processingEnvironment, "BIG_DECIMAL"));
    recognizers.put(_getOrThrowType(elementUtils, Number.class), primitiveResolver.resolve(processingEnvironment, "NUMBER"));
    recognizers.put(_getOrThrowArrayType(elementUtils, typeUtils, Byte.class), primitiveResolver.resolve(processingEnvironment, "BLOB"));

    // init atomics
    RecognizerInstance.Resolver atomicResolver = RecognizerInstance.resolver("ai.swim.structure.recognizer.std.AtomicRecognizer");
    recognizers.put(_getOrThrowType(elementUtils, AtomicBoolean.class), atomicResolver.resolve(processingEnvironment, "ATOMIC_BOOLEAN"));
    recognizers.put(_getOrThrowType(elementUtils, AtomicInteger.class), atomicResolver.resolve(processingEnvironment, "ATOMIC_INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, AtomicLong.class), atomicResolver.resolve(processingEnvironment, "ATOMIC_LONG"));

    return new RecognizerFactory(recognizers);
  }

  private static <T> String _getOrThrowType(Elements elementUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw classInitFailure(clazz);
    }

    return typeElement.asType().toString();
  }

  private static <T> String _getOrThrowArrayType(Elements elementUtils, Types typeUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw classInitFailure(clazz);
    }

    ArrayType arrayType = typeUtils.getArrayType(typeElement.asType());

    if (arrayType == null) {
      throw classInitFailure(clazz);
    }

    return arrayType.getComponentType().toString();
  }

  private static RuntimeException classInitFailure(Class<?> clazz) {
    return new RuntimeException("Failed to initialise recognizer factory with class: " + clazz);
  }

  public RecognizerModel lookup(TypeMirror typeMirror) {
    return this.recognizers.get(typeMirror.toString());
  }

  public RecognizerModel getOrInspect(Element element, ScopedContext context) {
    RecognizerModel map = this.recognizers.get(element.asType().toString());

    if (map != null) {
      return map;
    }

    if (element.getKind().isClass()) {
      return inspectAndInsertClass((TypeElement) element, new ScopedContext(context.getProcessingContext(), element));
    } else if (element.getKind().isInterface()) {
      return inspectAndInsertInterface(element, new ScopedContext(context.getProcessingContext(), element));
    } else {
      context.getMessager().error("Cannot inspect a: " + element.getKind());
      return null;
    }
  }

  private RecognizerModel inspectAndInsertInterface(Element element, ScopedContext context) {
    InterfaceMap interfaceMap = ElementInspector.inspectInterface(element, context);

    if (interfaceMap == null) {
      return null;
    }

    this.recognizers.put(element.asType().toString(), interfaceMap);
    return interfaceMap;
  }

  private RecognizerModel inspectAndInsertClass(TypeElement element, ScopedContext context) {
    if (!element.getKind().isClass()) {
      throw new RuntimeException("Element is not an interface: " + element);
    }

    ClassMap classMap = ElementInspector.inspectClass(element, context);

    if (classMap == null) {
      return null;
    }

    this.recognizers.put(element.asType().toString(), classMap);
    return classMap;
  }

}
