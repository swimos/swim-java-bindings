package ai.swim.structure.recognizer.untyped;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;

import java.util.HashMap;
import java.util.Map;

class UntypedMapRecognizer<T> extends Recognizer<T> {
  private final Map<Object, Object> map;
  private Object key;
  private State state;
  private Recognizer<Object> nested;

  UntypedMapRecognizer(Object key) {
    this.map = new HashMap<>();
    this.key = key;
    this.state = State.Value;
  }

  private enum State {
    Key,
    Slot,
    Value
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Key:
        if (event.isEndRecord() && this.nested == null) {
          return UntypedRecognizer.done(this, this.map);
        } else {
          if (this.nested == null) {
            this.nested = new UntypedRecognizer<>();
          }

          this.nested = this.nested.feedEvent(event);

          if (this.nested.isDone()) {
            this.key = this.nested.bind();
            this.nested = null;
            this.state = State.Slot;
            return this;
          } else if (this.nested.isCont()) {
            return this;
          } else if (this.nested.isError()) {
            return Recognizer.error(this.nested.trap());
          } else {
            throw new AssertionError();
          }
        }
      case Slot:
        if (event.isSlot()) {
          this.state = State.Value;
          return this;
        } else {
          return Recognizer.error(ReadEvent.slot(), event);
        }
      case Value:
        if (this.nested == null) {
          this.nested = new UntypedRecognizer<>();
        }

        this.nested = this.nested.feedEvent(event);

        if (this.nested.isDone()) {
          map.put(this.key, this.nested.bind());
          this.key = null;
          this.nested = null;
          this.state = State.Key;
          return this;
        } else if (this.nested.isCont()) {
          return this;
        } else if (this.nested.isError()) {
          return Recognizer.error(this.nested.trap());
        } else {
          throw new AssertionError();
        }
      default:
        throw new AssertionError(this.state);
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new UntypedRecognizer<>();
  }
}
