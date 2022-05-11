package ai.swim.structure.processor.context;

import ai.swim.structure.processor.recognizer.RecognizerFactory;

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

  public RecognizerFactory getFactory() {
    return factory;
  }

  @Override
  public String toString() {
    return "ProcessingContext{" +
        "processingEnvironment=" + processingEnvironment +
        '}';
  }

  public ScopedContext enter(Element root) {
    return new ScopedContext(this, root);
  }
}
