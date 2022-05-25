package ai.swim.structure.processor.context;

import ai.swim.structure.processor.recognizer.RecognizerFactory;
import ai.swim.structure.processor.recognizer.RecognizerModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;

public class ScopedContext {
  private final ProcessingContext processingContext;
  private final Element root;
  private final ScopedMessager messager;
  private final NameFactory formatter;

  public ScopedContext(ProcessingContext processingContext, Element root) {
    ProcessingEnvironment processingEnvironment = processingContext.getProcessingEnvironment();

    this.processingContext = processingContext;
    this.root = root;
    this.messager = new ScopedMessager(processingEnvironment.getMessager(), root);

    Elements elementUtils = processingEnvironment.getElementUtils();
    PackageElement packageElement = elementUtils.getPackageOf(root);

    this.formatter = new NameFactory(root.getSimpleName().toString(), packageElement);
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

  public NameFactory getNameFactory() {
    return formatter;
  }

  public ProcessingContext getProcessingContext() {
    return processingContext;
  }
}
