package ai.swim.structure.processor.context;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public class ProcessingContext {

  private final ProcessingEnvironment processingEnvironment;

  public ProcessingContext(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  @Override
  public String toString() {
    return "ProcessingContext{" +
        "processingEnvironment=" + processingEnvironment +
        '}';
  }
}
