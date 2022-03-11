package ai.swim.recon.utils;

public class EitherRight<L,R> extends Either<L,R> {

  private final R value;

  public EitherRight(R value) {
    this.value = value;
  }

  @Override
  public boolean isLeft() {
    return false;
  }

  @Override
  public boolean isRight() {
    return true;
  }

  public R value() {
    return this.value;
  }

}
