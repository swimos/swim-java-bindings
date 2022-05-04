package ai.swim.structure.processor.context;

import ai.swim.structure.processor.ElementInspector;
import ai.swim.structure.processor.ClassMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public class ProcessingContext {

  private final ProcessingEnvironment processingEnvironment;
  private final ElementInspector inspector;

  public ProcessingContext(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
    inspector = new ElementInspector();
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  public ClassMap getMap(Element element) {
    return this.inspector.getOrInspect(element, this.processingEnvironment);
  }

  @Override
  public String toString() {
    return "ProcessingContext{" +
        "processingEnvironment=" + processingEnvironment +
        '}';
  }
}
