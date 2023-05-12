package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.model.TypeInitializer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class Context<I extends TypeInitializer, N> {
  private final Element root;
  private final ProcessingEnvironment processingEnvironment;
  private final I initializer;
  private final N formatter;

  public Context(Element root, ProcessingEnvironment processingEnvironment, I initializer, N formatter) {
    this.root = root;
    this.processingEnvironment = processingEnvironment;
    this.initializer = initializer;
    this.formatter = formatter;
  }

  public Element getRoot() {
    return root;
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  public I getInitializer() {
    return initializer;
  }

  public N getFormatter() {
    return formatter;
  }

  public Elements getElementUtils() {
    return processingEnvironment.getElementUtils();
  }

  public Types getTypeUtils() {
    return processingEnvironment.getTypeUtils();
  }
}
