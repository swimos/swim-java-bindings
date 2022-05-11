package ai.swim.structure.processor.context;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class ScopedMessager {
  private final Messager messager;
  private final Element root;

  public ScopedMessager(Messager messager, Element root) {
    this.messager = messager;
    this.root = root;
  }

  public void log(Diagnostic.Kind kind, String event){
    String message = String.format("%s: %s", this.root, event);
    this.messager.printMessage(kind, message);
  }

  public void error(String event){
    this.log(Diagnostic.Kind.ERROR,event);
  }
}
