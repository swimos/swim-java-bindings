package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.bridge.RecognizerBridge;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.proxy.WriterProxy;

import java.util.function.Function;


/**
 * <h2>Registration</h2>>
 * It is preferred for recognizers to be manually registered with the recognizer proxy so that no reflection has to be
 * used each time it is used. Alternatively, a recognizer can be annotated with @Autoloaded(targetClass.class) and it
 * will be automatically registered with the recognizer proxy when it is initialised.
 *
 * <h2>Derivation</h2>
 * In most cases, a recognizer can be derived if it is annotated with @AutoForm. This will generate both a recognizer
 * and a builder for the target class.
 *
 * <h2>Structure</h2>
 * Recognizers that are accessed through the recognizer proxy or are used by classes that depend on it should contain
 * a no-arg constructor. If this is not possible, then a manual implementation of a recognizer is required.
 *
 * @param <T>
 */
public abstract class Recognizer<T> {

  public static <T> Recognizer<T> done(T target, Recognizer<T> delegate) {
    return new RecognizerDone<>(target, delegate);
  }

  public static <T> Recognizer<T> error(RuntimeException error) {
    return new RecognizerError<>(error);
  }

  public static <T> Recognizer<T> error(ReadEvent expected, ReadEvent actual) {
    return Recognizer.error(new RuntimeException(String.format("Expected: %s, found: %s", expected, actual)));
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

  /***
   * Reset this recognizer back to its initial state and return a new instance.
   */
  public abstract Recognizer<T> reset();

  public <Y> Recognizer<Y> map(Function<T, Y> mapFn) {
    return new MappingRecognizer<>(this, mapFn);
  }

  public Recognizer<T> required() {
    return new RecognizerRequired<>(this);
  }

  public Recognizer<T> asAttrRecognizer() {
    return new SimpleAttrBodyRecognizer<>(this);
  }

  public Recognizer<T> asBodyRecognizer() {
    return new SimpleRecBodyRecognizer<>(this);
  }

  public <W> T transform(W value, Writable<W> writable) {
    return writable.writeInto(value, new RecognizerBridge<>(this));
  }

  public <W> T transform(W value) {
    return transform(value, WriterProxy.getProxy().lookupObject(value));
  }

  public T flush() {
    return null;
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
    throw new IllegalStateException();
  }

  @Override
  public Recognizer<T> reset() {
    throw new IllegalStateException();
  }
}