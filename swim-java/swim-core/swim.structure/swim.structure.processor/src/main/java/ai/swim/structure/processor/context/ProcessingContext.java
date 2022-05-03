package ai.swim.structure.processor.context;

import ai.swim.structure.processor.ElementInspector;
import ai.swim.structure.processor.ElementMap;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.util.Map;

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

  public ElementMap getMap(Element element) {
    return this.inspector.getOrInspect(element, this.processingEnvironment);
  }

  @Override
  public String toString() {
    return "ProcessingContext{" +
        "processingEnvironment=" + processingEnvironment +
        '}';
  }
}
