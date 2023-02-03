package ai.swim.structure.recognizer.std.collections;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.StructuralRecognizer;

import java.util.Collection;

public abstract class CollectionRecognizer<T, E extends Collection<T>, O> extends StructuralRecognizer<O> {

  protected final E collection;
  protected final boolean isAttrBody;
  protected Recognizer<T> delegate;
  protected State state;

  protected CollectionRecognizer(Recognizer<T> delegate, E collection) {
    this(delegate, collection, false);
  }

  protected CollectionRecognizer(Recognizer<T> delegate, E collection, boolean isAttrBody) {
    this.delegate = delegate;
    this.isAttrBody = isAttrBody;
    this.collection = collection;
    this.state = State.Init;
  }

  @Override
  public Recognizer<O> feedEvent(ReadEvent event) {
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
          return Recognizer.done(map(this.collection), this);
        } else if (event.isEndAttribute() && this.isAttrBody) {
          return Recognizer.done(map(this.collection), this);
        } else {
          this.state = State.Item;
          return this.feedElement(event);
        }
      default:
        throw new AssertionError(event);
    }
  }

  protected abstract O map(E collection);

  private Recognizer<O> feedElement(ReadEvent event) {
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

  private enum State {
    Init, Item, Between
  }


}
