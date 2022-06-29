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
import java.util.List;
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
    RecognizerReference.Formatter primitiveFormatter = new RecognizerReference.Formatter("ai.swim.structure.recognizer.std.ScalarRecognizer");
    recognizers.put(_getOrThrowType(elementUtils, Byte.class), primitiveFormatter.recognizerFor("BYTE"));
    recognizers.put(_getOrThrowType(elementUtils, Short.class), primitiveFormatter.recognizerFor("SHORT"));
    recognizers.put(_getOrThrowType(elementUtils, Integer.class), primitiveFormatter.recognizerFor("INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, Long.class), primitiveFormatter.recognizerFor("LONG"));
    recognizers.put(_getOrThrowType(elementUtils, Float.class), primitiveFormatter.recognizerFor("FLOAT"));
    recognizers.put(_getOrThrowType(elementUtils, Boolean.class), primitiveFormatter.recognizerFor("BOOLEAN"));
    recognizers.put(_getOrThrowType(elementUtils, String.class), primitiveFormatter.recognizerFor("STRING"));
    recognizers.put(_getOrThrowType(elementUtils, Character.class), primitiveFormatter.recognizerFor("CHARACTER"));
    recognizers.put(_getOrThrowType(elementUtils, BigInteger.class), primitiveFormatter.recognizerFor("BIG_INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, BigDecimal.class), primitiveFormatter.recognizerFor("BIG_DECIMAL"));
    recognizers.put(_getOrThrowType(elementUtils, Number.class), primitiveFormatter.recognizerFor("NUMBER"));
    recognizers.put(_getOrThrowArrayType(elementUtils, typeUtils, Byte.class), primitiveFormatter.recognizerFor("BLOB"));

    // init atomics
    RecognizerReference.Formatter atomicFormatter = new RecognizerReference.Formatter("ai.swim.structure.recognizer.std.AtomicRecognizer");
    recognizers.put(_getOrThrowType(elementUtils, AtomicBoolean.class), atomicFormatter.recognizerFor("ATOMIC_BOOLEAN"));
    recognizers.put(_getOrThrowType(elementUtils, AtomicInteger.class), atomicFormatter.recognizerFor("ATOMIC_INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, AtomicLong.class), atomicFormatter.recognizerFor("ATOMIC_LONG"));

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

  public void addAll(List<ClassMap> derived) {
    for (ClassMap classMap : derived) {
      recognizers.put(classMap.getRoot().asType().toString(), classMap);
    }
  }

}
