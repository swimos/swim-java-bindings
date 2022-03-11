package ai.swim.recon.utils;

import java.util.Objects;

public class EitherRight<L,R> extends Either<L,R> {

  private final R value;

  public EitherRight(R value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EitherRight<?, ?> that = (EitherRight<?, ?>) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
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
