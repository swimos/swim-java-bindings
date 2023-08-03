package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.model.TypeInitializer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Scoped context to the root processing element.
 *
 * @param <I> the type initializer for transforming between a model and either a recognizer or a writer.
 * @param <N> the name formatter for the recognizer or writer.
 */
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

  /**
   * Returns the root processing element.
   */
  public Element getRoot() {
    return root;
  }

  /**
   * Returns the processing environment.
   */
  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  /**
   * Returns the type initializer.
   */
  public I getInitializer() {
    return initializer;
  }

  /**
   * Returns the name formatter.
   */
  public N getFormatter() {
    return formatter;
  }

  /**
   * Returns the element utilities.
   */
  public Elements getElementUtils() {
    return processingEnvironment.getElementUtils();
  }

  /**
   * Returns the type utilities.
   */
  public Types getTypeUtils() {
    return processingEnvironment.getTypeUtils();
  }
}
