package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.inspect.ElementInspector;
import ai.swim.structure.processor.context.ProcessingContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.HashMap;

public class RecognizerFactory {
  private final HashMap<String, RecognizerModel> recognizers;

  private RecognizerFactory(HashMap<String, RecognizerModel> recognizers) {
    this.recognizers = recognizers;
  }

  public static RecognizerFactory initFrom(ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    HashMap<String, RecognizerModel> recognizers = new HashMap<>();

    // init core types
    RecognizerReference.Formatter formatter = new RecognizerReference.Formatter("ai.swim.structure.recognizer.ScalarRecognizer");
    recognizers.put(_getOrThrow(elementUtils, Integer.class), formatter.recognizerFor("BOXED_INTEGER"));
    recognizers.put(_getOrThrow(elementUtils, Long.class), formatter.recognizerFor("BOXED_LONG"));
    recognizers.put(_getOrThrow(elementUtils, Float.class), formatter.recognizerFor("BOXED_FLOAT"));
    recognizers.put(_getOrThrow(elementUtils, Boolean.class), formatter.recognizerFor("BOXED_BOOLEAN"));
    recognizers.put(_getOrThrow(elementUtils, String.class), formatter.recognizerFor("STRING"));

    return new RecognizerFactory(recognizers);
  }

  private static <T> String _getOrThrow(Elements elementUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw new RuntimeException("Failed to initialise recognizer factory with class: " + clazz);
    }

    return typeElement.asType().toString();
  }

  public RecognizerModel lookup(TypeMirror element) {
    return this.recognizers.get(element.toString());
  }

  public RecognizerModel getOrInspect(Element element, ProcessingContext context) {
    RecognizerModel map = this.recognizers.get(element.toString());

    if (map != null) {
      return map;
    }

    return inspectAndInsertClass(element, context);
  }

  private RecognizerModel inspectAndInsertClass(Element element, ProcessingContext context) {
    ClassMap classMap = ElementInspector.inspect(element, context);

    if (classMap == null) {
      return null;
    }

    this.recognizers.put(element.asType().toString(), classMap);
    return classMap;
  }
}
