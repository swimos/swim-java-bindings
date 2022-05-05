package ai.swim.structure.processor.context;

import ai.swim.structure.processor.recognizer.RecognizerFactory;
import ai.swim.structure.processor.recognizer.RecognizerModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public class ProcessingContext {

  private final ProcessingEnvironment processingEnvironment;
  private final RecognizerFactory factory;

  public ProcessingContext(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
    this.factory = RecognizerFactory.initFrom(processingEnvironment);
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  public RecognizerModel getRecognizer(Element element) {
    return this.factory.getOrInspect(element, this);
  }

  public RecognizerFactory getFactory() {
    return factory;
  }

  @Override
  public String toString() {
    return "ProcessingContext{" +
        "processingEnvironment=" + processingEnvironment +
        '}';
  }
}
