package ai.swim.structure.processor.context;

import ai.swim.structure.processor.recognizer.RecognizerFactory;
import ai.swim.structure.processor.recognizer.RecognizerModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public class ScopedContext {
  private final ProcessingContext processingContext;
  private final Element root;
  private final ScopedMessager messager;
  private final NameFormatter formatter;

  public ScopedContext(ProcessingContext processingContext, Element root) {
    this.processingContext = processingContext;
    this.root = root;
    this.messager = new ScopedMessager(this.processingContext.getProcessingEnvironment().getMessager(), root);
    this.formatter = new NameFormatter(root.getSimpleName().toString());
  }

  public Element getRoot() {
    return root;
  }

  public ScopedMessager getMessager() {
    return messager;
  }

  public RecognizerFactory getRecognizerFactory() {
    return this.processingContext.getFactory();
  }

  public RecognizerModel getRecognizer(Element element) {
    return this.processingContext.getFactory().getOrInspect(element, this);
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return this.processingContext.getProcessingEnvironment();
  }

  public RecognizerFactory getFactory() {
    return this.processingContext.getFactory();
  }

  public NameFormatter getFormatter() {
    return formatter;
  }
}
