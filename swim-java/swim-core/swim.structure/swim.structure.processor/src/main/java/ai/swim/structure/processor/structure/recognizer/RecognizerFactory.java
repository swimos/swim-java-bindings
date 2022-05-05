package ai.swim.structure.processor.structure.recognizer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.HashMap;

public class RecognizerFactory {
  private final HashMap<TypeMirror, RecognizerModel> recognizers;

  private RecognizerFactory(HashMap<TypeMirror, RecognizerModel> recognizers) {
    this.recognizers = recognizers;
  }

  public static RecognizerFactory initFrom(ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    HashMap<TypeMirror, RecognizerModel> recognizers = new HashMap<>();

    // init core types
    RecognizerReference.RecognizerReferenceFactory factory = new RecognizerReference.RecognizerReferenceFactory("ai.swim.structure.recognizer.ScalarRecognizer");
    recognizers.put(_getOrThrow(elementUtils, Integer.class), factory.recognizerFor("BOXED_INTEGER"));
    recognizers.put(_getOrThrow(elementUtils, Long.class), factory.recognizerFor("BOXED_LONG"));
    recognizers.put(_getOrThrow(elementUtils, Float.class),factory.recognizerFor("BOXED_FLOAT"));
    recognizers.put(_getOrThrow(elementUtils, Boolean.class), factory.recognizerFor("BOXED_BOOLEAN"));
    recognizers.put(_getOrThrow(elementUtils, String.class), factory.recognizerFor("STRING"));

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
}
