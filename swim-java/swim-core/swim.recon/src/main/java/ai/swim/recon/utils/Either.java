package ai.swim.recon.utils;

public abstract class Either<L, R> {

  public static <EL, ER> Either<EL, ER> left(EL value) {
    return new EitherLeft<>(value);
  }

  public static <EL, ER> Either<EL, ER> right(ER value) {
    return new EitherRight<>(value);
  }

  public abstract boolean isLeft();

  public abstract boolean isRight();



}
