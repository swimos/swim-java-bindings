package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

public class SimpleRecBodyRecognizer<T> extends Recognizer<T> {
  private Recognizer<T> delegate;
  private State state;

  public SimpleRecBodyRecognizer(Recognizer<T> delegate) {
    this.delegate = delegate;
    this.state = State.Init;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        if (event.isStartBody()) {
          this.state = State.ReadingValue;
          return this;
        } else {
          return Recognizer.error(new RuntimeException("Expected a record body"));
        }
      case ReadingValue:
        this.delegate = this.delegate.feedEvent(event);
        if (this.delegate.isDone()) {
          this.state = State.AfterValue;
        } else if (this.delegate.isError()) {
          return Recognizer.error(this.delegate.trap());
        }

        return this;
      case AfterValue:
        if (event.isEndRecord()) {
          return Recognizer.done(this.delegate.bind(), this);
        } else {
          return Recognizer.error(new RuntimeException("Expected an end of record"));
        }
      default:
        throw new AssertionError();
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new SimpleRecBodyRecognizer<>(this.delegate.reset());
  }

  enum State {
    Init,
    ReadingValue,
    AfterValue
  }
}
