package ai.swim.structure.processor.structure.recognizer;

import ai.swim.structure.processor.ClassMap;
import ai.swim.structure.processor.ElementInspector;
import ai.swim.structure.processor.context.ProcessingContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.HashMap;
import java.util.Map;

public class RecognizerFactory {
  private final HashMap<TypeMirror, RecognizerModel> recognizers;
  private final Map<String, ClassMap> classMap;

  private RecognizerFactory(HashMap<TypeMirror, RecognizerModel> recognizers) {
    this.recognizers = recognizers;
    classMap = new HashMap<>();
  }

  public static RecognizerFactory initFrom(ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    HashMap<TypeMirror, RecognizerModel> recognizers = new HashMap<>();

    // init core types
    RecognizerReference.Formatter formatter = new RecognizerReference.Formatter("ai.swim.structure.recognizer.ScalarRecognizer");
    recognizers.put(_getOrThrow(elementUtils, Integer.class), formatter.recognizerFor("BOXED_INTEGER"));
    recognizers.put(_getOrThrow(elementUtils, Long.class), formatter.recognizerFor("BOXED_LONG"));
    recognizers.put(_getOrThrow(elementUtils, Float.class), formatter.recognizerFor("BOXED_FLOAT"));
    recognizers.put(_getOrThrow(elementUtils, Boolean.class), formatter.recognizerFor("BOXED_BOOLEAN"));
    recognizers.put(_getOrThrow(elementUtils, String.class), formatter.recognizerFor("STRING"));

    return new RecognizerFactory(recognizers);
  }

  private static <T> TypeMirror _getOrThrow(Elements elementUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw new RuntimeException("Failed to initialise recognizer factory with class: " + clazz);
    }

    return typeElement.asType();
  }

  public RecognizerModel lookup(TypeMirror element) {
    return this.recognizers.get(element);
  }

  public ClassMap getOrInspect(Element element, ProcessingContext context) {
    ClassMap map = this.classMap.get(element.toString());

    if (map != null) {
      return map;
    }

    return inspectAndInsertClass(element, context);
  }

  private ClassMap inspectAndInsertClass(Element element, ProcessingContext context) {
    ClassMap classMap = ElementInspector.inspect(element, context);

    if (classMap == null) {
      return null;
    }

    this.classMap.put(element.toString(), classMap);
    return classMap;
  }
}
