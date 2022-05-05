package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

import java.util.function.Function;

// todo: number recognizers will truncate values
public abstract class Recognizer<T> {

  public static <T> Recognizer<T> done(T target, Recognizer<T> delegate) {
    return new RecognizerDone<>(target, delegate);
  }

  public static <T> Recognizer<T> error(RuntimeException error) {
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

  public RuntimeException trap() {
    throw new IllegalStateException();
  }

  public abstract Recognizer<T> reset();

  public <Y> Recognizer<Y> map(Function<T, Y> mapFn) {
    return new MappingRecognizer<>(this, mapFn);
  }

  public Recognizer<T> required() {
    return new RecognizerRequired<>(this);
  }

  /***
   * Returns whether this recognizer has been run at least once.
   */
  public boolean hasInit() {
    return true;
  }
}

final class RecognizerDone<T> extends Recognizer<T> {

  final T target;
  private final Recognizer<T> delegate;

  RecognizerDone(T target, Recognizer<T> delegate) {
    this.target = target;
    this.delegate = delegate;
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

  @Override
  public Recognizer<T> reset() {
    return this.delegate.reset();
  }
}

final class RecognizerError<T> extends Recognizer<T> {

  final RuntimeException error;

  RecognizerError(RuntimeException error) {
    this.error = error;
  }

  @Override
  public RuntimeException trap() {
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

  @Override
  public Recognizer<T> reset() {
    throw new IllegalStateException();
  }
}