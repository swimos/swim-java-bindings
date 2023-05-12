package ai.swim.recon.result;

import java.util.Objects;

public class ResultOk<O> extends ParseResult<O> {
  private final O item;

  public ResultOk(O item) {
    this.item = item;
  }

  @Override
  public O bind() {
    return this.item;
  }

  @Override
  public boolean isOk() {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ParseResult<T> cast() {
    return new ResultOk<>((T) this.item);
  }

  @Override
  public String toString() {
    return "ResultOk{" +
      "item=" + item +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultOk<?> resultOk = (ResultOk<?>) o;
    return Objects.equals(item, resultOk.item);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item);
  }
}
