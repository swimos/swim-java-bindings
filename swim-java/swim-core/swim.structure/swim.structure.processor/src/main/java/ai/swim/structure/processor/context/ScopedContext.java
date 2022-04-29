package ai.swim.structure.processor.context;

import javax.annotation.processing.Messager;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;

public class ScopedContext {
  private final ProcessingContext processingContext;
  private final DeclaredType root;

  public ScopedContext(ProcessingContext processingContext, DeclaredType root) {
    this.processingContext = processingContext;
    this.root = root;
  }

  public ProcessingContext getProcessingContext() {
    return processingContext;
  }

  public DeclaredType getRoot() {
    return root;
  }

  public Messager getMessager() {
    return this.processingContext.getProcessingEnvironment().getMessager();
  }

  public void log(Diagnostic.Kind kind, String event) {
    String message = String.format("%s: %s", this.root, event);
    this.getMessager().printMessage(kind, message);
  }

}
