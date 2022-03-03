package ai.swim.structure.form.recognizer;

import ai.swim.structure.form.event.ReadEvent;

public abstract class Recognizer<T> {

  public static <T> Recognizer<T> done(T target) {
    return new RecognizerDone<>(target);
  }

  public static <T> Recognizer<T> error(Exception error) {
    return new RecognizerError<>(error);
  }

  public abstract Recognizer<T> feedEvent(ReadEvent event);

  public boolean isCont() {
    return true;
  }

  public boolean isDone() {
    return false;
  }

  public boolean isError() {
    return false;
  }

  public T bind() {
    throw new IllegalStateException();
  }

  public Exception trap() {
    throw new IllegalStateException();
  }
}

final class RecognizerDone<T> extends Recognizer<T> {
  final T target;

  RecognizerDone(T target) {
    this.target = target;
  }

  @Override
  public boolean isCont() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public T bind() {
    return this.target;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    return this;
  }
}

final class RecognizerError<T> extends Recognizer<T> {
  final Exception error;

  RecognizerError(Exception error) {
    this.error = error;
  }

  @Override
  public Exception trap() {
    return this.error;
  }

  @Override
  public boolean isCont() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    return this;
  }
}