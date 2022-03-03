package ai.swim.structure.form;

public abstract class Parser {

  public boolean isCont() {
    return true;
  }

  public boolean isDone() {
    return false;
  }

  public boolean isError() {
    return false;
  }

}
