package ai.swim.structure.recognizer.untyped;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;

import java.util.ArrayList;
import java.util.List;

class UntypedListRecognizer<T> extends Recognizer<T> {
  private final List<Object> list;
  private Recognizer<Object> nested;

  UntypedListRecognizer(Object first) {
    this.list = new ArrayList<>(List.of(first));
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isEndRecord() && this.nested == null) {
      return UntypedRecognizer.done(this, list);
    }

    if (this.nested == null) {
      this.nested = new UntypedRecognizer<>();
    }

    this.nested = this.nested.feedEvent(event);

    if (this.nested.isDone()) {
      list.add(this.nested.bind());
      this.nested = null;
      return this;
    } else if (this.nested.isCont()) {
      return this;
    } else if (this.nested.isError()) {
      return Recognizer.error(this.nested.trap());
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new UntypedRecognizer<>();
  }

}
