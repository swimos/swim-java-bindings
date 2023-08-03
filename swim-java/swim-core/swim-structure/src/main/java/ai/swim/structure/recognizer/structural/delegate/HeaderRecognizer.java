package ai.swim.structure.recognizer.structural.delegate;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.structure.FieldRecognizingBuilder;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.FirstOf;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.IndexFn;
import java.util.BitSet;
import java.util.function.Supplier;

public class HeaderRecognizer<T> extends Recognizer<T> {
  private final boolean hasBody;
  private final boolean flattened;
  private final RecognizingBuilder<T> builder;
  private final BitSet bitSet;
  private final IndexFn<HeaderFieldKey> indexFn;
  private State state;
  private int index;

  public HeaderRecognizer(boolean hasBody,
      boolean flattened,
      RecognizingBuilder<T> builder,
      int slotCount,
      IndexFn<HeaderFieldKey> indexFn) {
    this.hasBody = hasBody;
    this.flattened = flattened;
    this.builder = builder;
    this.bitSet = new BitSet(!hasBody ? slotCount : slotCount + 1);
    this.indexFn = indexFn;
    this.index = 0;
    this.state = flattened ? !hasBody ? State.BetweenSlots : State.ExpectingBody : State.Init;
  }

  public static <T> RecognizingBuilder<T> headerBuilder(boolean hasBody,
      Supplier<RecognizingBuilder<T>> builder,
      int numSlots,
      IndexFn<HeaderFieldKey> indexFn) {
    return new FieldRecognizingBuilder<>(new FirstOf<>(
        new HeaderRecognizer<>(hasBody, true, builder.get(), numSlots, indexFn),
        new HeaderRecognizer<>(hasBody, false, builder.get(), numSlots, indexFn)
    ));
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        return onInit(event);
      case ExpectingBody:
        return onExpectingBody(event);
      case SlotItem:
      case BodyItem:
        return onItem(event);
      case BetweenSlots:
        return onBetweenSlots(event);
      case ExpectingSlot:
        return onExpectingSlot(event);
      case End:
        return onEnd(event);
      default:
        throw new AssertionError("Unexpected value: " + this.state);
    }
  }

  private Recognizer<T> onInit(ReadEvent event) {
    if (event.isStartBody()) {
      this.state = !this.hasBody ? State.BetweenSlots : State.ExpectingBody;
      return this;
    } else {
      return Recognizer.error(new RuntimeException("Unexpected state"));
    }
  }

  private Recognizer<T> onExpectingBody(ReadEvent event) {
    if (this.flattened && event.isEndAttribute()) {
      return Recognizer.done(this.builder.bind(), this);
    } else if (this.flattened && event.isEndRecord()) {
      this.state = State.End;
      return this;
    }

    Integer idx = this.indexFn.selectIndex(HeaderFieldKey.HEADER_BODY);
    if (idx != null) {
      this.index = idx;
      this.state = State.BodyItem;
      return onItem(event);
    } else {
      return Recognizer.error(new RuntimeException("Unexpected state"));
    }
  }

  private Recognizer<T> onItem(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.state = State.BetweenSlots;
        this.bitSet.set(this.index);
      }
      return this;
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

  private Recognizer<T> onBetweenSlots(ReadEvent event) {
    if (event.isEndAttribute() && this.flattened) {
      return Recognizer.done(this.builder.bind(), this);
    } else if (event.isEndRecord() && !this.flattened) {
      this.state = State.End;
      return this;
    } else if (event.isText()) {
      ReadTextValue textValue = (ReadTextValue) event;
      Integer idx = this.indexFn.selectIndex(HeaderFieldKey.slot(textValue.getValue()));

      if (idx != null) {
        if (!this.bitSet.get(idx)) {
          this.index = idx;
          this.state = State.ExpectingSlot;

          return this;
        } else {
          return Recognizer.error(new RuntimeException("Duplicate key: " + textValue.getValue()));
        }
      }
    }

    return Recognizer.error(new RuntimeException("Unexpected state"));
  }

  private Recognizer<T> onExpectingSlot(ReadEvent event) {
    if (event.isSlot()) {
      this.state = State.SlotItem;
      return this;
    } else {
      return Recognizer.error(new RuntimeException("Unexpected state"));
    }
  }

  private Recognizer<T> onEnd(ReadEvent event) {
    if (event.isEndAttribute()) {
      return Recognizer.done(this.builder.bind(), this);
    } else {
      return Recognizer.error(new RuntimeException("Unexpected state"));
    }
  }

  @Override
  public Recognizer<T> reset() {
    int fieldCount = this.bitSet.size();
    return new HeaderRecognizer<>(
        this.hasBody,
        this.flattened,
        this.builder.reset(),
        !this.hasBody ? fieldCount : fieldCount - 1,
        this.indexFn);
  }

  enum State {
    Init,
    ExpectingBody,
    BodyItem,
    BetweenSlots,
    ExpectingSlot,
    SlotItem,
    End,
  }
}
