package ai.swim.codec.input;

public abstract class Collector<I, O> {

  public abstract O collect();

  public abstract O collect(int n);

}
