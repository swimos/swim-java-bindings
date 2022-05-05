package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

public class RecognizerInit<T> extends Recognizer<T>{

  private final Recognizer<T> delegate;

  public RecognizerInit(Recognizer<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    return this.delegate.feedEvent(event);
  }

  @Override
  public Recognizer<T> reset() {
    return this;
  }

  @Override
  public boolean hasInit() {
    return false;
  }
}
