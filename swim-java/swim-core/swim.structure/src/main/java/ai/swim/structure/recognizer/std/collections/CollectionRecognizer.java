package ai.swim.structure.recognizer.std.collections;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;

import java.util.Collection;

public abstract class CollectionRecognizer<T, E extends Collection<T>> extends Recognizer<E> {

  protected Recognizer<T> delegate;
  protected final E collection;
  protected State state;
  protected final boolean isAttrBody;

  private enum State {
    Init, Item, Between
  }

  protected CollectionRecognizer(Recognizer<T> delegate, E collection) {
    this(delegate, collection, false);
  }

  protected CollectionRecognizer(Recognizer<T> delegate, E collection, boolean isAttrBody) {
    this.delegate = delegate;
    this.isAttrBody = isAttrBody;
    this.collection = collection;
    state = State.Init;
  }

  @Override
  public Recognizer<E> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        if (event.isStartBody()) {
          this.state = State.Between;
          return this;
        } else {
          return Recognizer.error(new RuntimeException("Expected a record body"));
        }
      case Item:
        return this.feedElement(event);
      case Between:
        if (event.isEndRecord() && !this.isAttrBody) {
          return Recognizer.done(this.collection, this);
        } else if (event.isEndAttribute() && this.isAttrBody) {
          return Recognizer.done(this.collection, this);
        } else {
          this.state = State.Item;
          return this.feedElement(event);
        }
      default:
        throw new AssertionError(event);
    }
  }

  private Recognizer<E> feedElement(ReadEvent event) {
    this.delegate = this.delegate.feedEvent(event);

    if (this.delegate.isDone()) {
      this.collection.add(this.delegate.bind());
      this.delegate = this.delegate.reset();
      this.state = State.Between;
    } else if (this.delegate.isError()) {
      return Recognizer.error(this.delegate.trap());
    }

    return this;
  }


}
