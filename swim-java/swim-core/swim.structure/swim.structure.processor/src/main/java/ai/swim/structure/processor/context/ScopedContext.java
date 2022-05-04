package ai.swim.structure.processor.context;

import ai.swim.structure.processor.structure.recognizer.RecognizerFactory;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.util.Locale;

public class ScopedContext {
  private final ProcessingContext processingContext;
  private final Element root;
  private final RecognizerFactory recognizerFactory;

  public ScopedContext(ProcessingContext processingContext, Element root) {
    this.processingContext = processingContext;
    this.root = root;
    this.recognizerFactory = RecognizerFactory.initFrom(processingContext.getProcessingEnvironment());
  }

  public ProcessingContext getProcessingContext() {
    return processingContext;
  }

  public Element getRoot() {
    return root;
  }

  public Messager getMessager() {
    return this.processingContext.getProcessingEnvironment().getMessager();
  }

  public void log(Diagnostic.Kind kind, String event) {
    String message = String.format("%s: %s", this.root, event);
    this.getMessager().printMessage(kind, message);
  }

  public RecognizerFactory getRecognizerFactory() {
    return recognizerFactory;
  }
}
