/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.util;

import java.util.Objects;

public abstract class Either<L, R> {
  private Either() {

  }

  public static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  public static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  public abstract boolean isLeft();

  public abstract boolean isRight();

  public abstract <O> O accept(Visitor<L, R, O> visitor);

  public abstract void peek(Peek<L, R> peekable);

  public L unwrapLeft() {
    if (isLeft()) {
      return ((Left<L, R>) this).value;
    } else {
      throw new IllegalStateException("Attempted to unwrap a left value on: " + this);
    }
  }

  public R unwrapRight() {
    if (isRight()) {
      return ((Right<L, R>) this).value;
    } else {
      throw new IllegalStateException("Attempted to unwrap a right value on: " + this);
    }
  }

  public interface Visitor<L, R, O> {
    O visitLeft(L value);

    O visitRight(R value);
  }

  public interface Peek<L, R> {
    void peekLeft(L value);

    void peekRight(R value);
  }

  private static class Left<L, R> extends Either<L, R> {
    private final L value;

    private Left(L value) {
      this.value = value;
    }

    public L getValue() {
      return value;
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public <O> O accept(Visitor<L, R, O> visitor) {
      return visitor.visitLeft(value);
    }

    @Override
    public void peek(Peek<L, R> peekable) {
      peekable.peekLeft(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Left<?, ?> left = (Left<?, ?>) o;
      return Objects.equals(value, left.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Left{" +
          "value=" + value +
          '}';
    }
  }

  private static class Right<L, R> extends Either<L, R> {
    private final R value;

    private Right(R value) {
      this.value = value;
    }

    public R getValue() {
      return value;
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public <O> O accept(Visitor<L, R, O> visitor) {
      return visitor.visitRight(value);
    }

    @Override
    public void peek(Peek<L, R> peekable) {
      peekable.peekRight(value);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Right<?, ?> right = (Right<?, ?>) o;
      return Objects.equals(value, right.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "Right{" +
          "value=" + value +
          '}';
    }
  }
}
