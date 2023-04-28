package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

public class RecognizerRequired<T> extends Recognizer<T> {
  private Recognizer<T> delegate;

  public RecognizerRequired(Recognizer<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    this.delegate = this.delegate.feedEvent(event);
    if (this.delegate.isDone()) {
      T output = this.delegate.bind();
      if (output == null) {
        return Recognizer.error(new NullPointerException());
      } else {
        return Recognizer.done(output, this);
      }
    } else if (this.delegate.isError()) {
      return Recognizer.error(this.delegate.trap());
    }

    return this;
  }

  @Override
  public Recognizer<T> reset() {
    return new RecognizerRequired<>(this.delegate.reset());
  }

}
