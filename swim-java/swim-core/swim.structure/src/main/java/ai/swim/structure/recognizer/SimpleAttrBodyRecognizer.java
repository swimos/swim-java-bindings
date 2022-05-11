package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

public class SimpleAttrBodyRecognizer<T> extends Recognizer<T> {
  private Recognizer<T> delegate;
  private boolean afterContent;

  public SimpleAttrBodyRecognizer(Recognizer<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (this.afterContent) {
      if (event.isEndAttribute()) {
        return Recognizer.done(this.delegate.bind(), this);
      } else {
        return Recognizer.error(new RuntimeException("Expected an end attribute"));
      }
    } else {
      this.delegate = this.delegate.feedEvent(event);

      if (this.delegate.isDone()) {
        this.afterContent = true;
        return this;
      } else if (this.delegate.isError()) {
        return Recognizer.error(this.delegate.trap());
      } else {
        return this;
      }
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new SimpleRecBodyRecognizer<>(this.delegate.reset());
  }
}
