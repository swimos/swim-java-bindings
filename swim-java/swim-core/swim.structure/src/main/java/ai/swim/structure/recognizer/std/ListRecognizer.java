package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class ListRecognizer<E> extends Recognizer<List<E>> {

  private Recognizer<E> delegate;
  private final List<E> list;
  private State state;
  private final boolean isAttrBody;

  private enum State {
    Init, Item, Between
  }

  public ListRecognizer(Recognizer<E> delegate, boolean isAttrBody) {
    this.delegate = delegate;
    this.isAttrBody = isAttrBody;
    list = new ArrayList<>();
    state = State.Init;
  }

  @Override
  public Recognizer<List<E>> feedEvent(ReadEvent event) {
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
          return Recognizer.done(this.list, this);
        } else if (event.isEndAttribute() && this.isAttrBody) {
          return Recognizer.done(this.list, this);
        } else {
          this.state = State.Item;
          return this.feedElement(event);
        }
    }

    return this;
  }

  private Recognizer<List<E>> feedElement(ReadEvent event) {
    this.delegate = this.delegate.feedEvent(event);

    if (this.delegate.isDone()) {
      this.list.add(this.delegate.bind());
      this.delegate = this.delegate.reset();
      this.state = State.Between;
    } else if (this.delegate.isError()) {
      return Recognizer.error(this.delegate.trap());
    }

    return this;
  }

  @Override
  public Recognizer<List<E>> reset() {
    return new ListRecognizer<>(this.delegate.reset(), isAttrBody);
  }
}
