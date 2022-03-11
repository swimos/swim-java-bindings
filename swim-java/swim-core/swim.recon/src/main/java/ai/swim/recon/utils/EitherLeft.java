package ai.swim.recon.utils;

public class EitherLeft<L,R> extends Either<L,R> {

  private final L value;

  public  EitherLeft(L value) {
    this.value = value;
  }

  @Override
  public boolean isLeft() {
    return true;
  }

  @Override
  public boolean isRight() {
    return false;
  }

  public L value() {
    return this.value;
  }

}
