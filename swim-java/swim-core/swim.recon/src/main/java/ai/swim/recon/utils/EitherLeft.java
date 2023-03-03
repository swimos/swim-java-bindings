package ai.swim.recon.utils;

import java.util.Objects;

public class EitherLeft<L, R> extends Either<L, R> {

  private final L value;

  public EitherLeft(L value) {
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
    EitherLeft<?, ?> that = (EitherLeft<?, ?>) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
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

  @Override
  public String toString() {
    return "EitherLeft{" +
        "value=" + value +
        '}';
  }

}
