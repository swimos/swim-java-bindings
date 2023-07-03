package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;

public abstract class SimpleRecognizer<T> extends Recognizer<T> {
  private final boolean allowExtant;
  private final String type;

  public SimpleRecognizer(boolean allowExtant, String type) {
    this.allowExtant = allowExtant;
    this.type = type;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (allowExtant && event.isExtant()) {
      return Recognizer.done(null, this);
    } else {
      T value = feed(event);
      if (value == null) {
        return Recognizer.error(new RuntimeException(String.format(String.format("Found '%s', expected: '%s'", event, type))));
      } else {
        return Recognizer.done(value, this);
      }
    }
  }

  protected abstract T feed(ReadEvent event);

  @Override
  public Recognizer<T> reset() {
    return this;
  }
}
