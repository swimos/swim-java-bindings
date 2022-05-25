package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.ClassMap;
import ai.swim.structure.processor.inspect.ElementInspector;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.HashMap;
import java.util.List;

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

  public RecognizerModel lookup(Element element) {
    return this.recognizers.get(element.asType().toString());
  }

  public RecognizerModel getOrInspect(Element element, ScopedContext context) {
    RecognizerModel map = this.recognizers.get(element.asType().toString());

    if (map != null) {
      return map;
    }

    return inspectAndInsertClass(element, new ScopedContext(context.getProcessingContext(), element));
  }

  private RecognizerModel inspectAndInsertClass(Element element, ScopedContext context) {
    ClassMap classMap = ElementInspector.inspect(element, context);

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
