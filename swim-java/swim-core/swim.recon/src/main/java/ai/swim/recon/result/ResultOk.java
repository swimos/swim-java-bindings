package ai.swim.recon.result;

public class ResultOk<O> extends ParseResult<O> {
  private final O item;

  public  ResultOk(O item) {
    this.item= item;
  }
}
