package ai.swim.structure.recognizer.structural.labelled;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.ClassRecognizer;
import ai.swim.structure.recognizer.structural.IndexFn;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class LabelledClassRecognizer<T> extends ClassRecognizer<LabelledClassRecognizer.State, LabelledFieldKey, T> {

  public LabelledClassRecognizer(TagSpec tagSpec, RecognizingBuilder<T> builder, int fieldCount, IndexFn<LabelledFieldKey> indexFn) {
    super(tagSpec, builder, fieldCount, indexFn, State.Init);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        return onInit(event, LabelledFieldKey.TAG);
      case Header:
        return onHeader(event, State.AttrBetween);
      case NoHeader:
        return onNoHeader(event, State.AttrBetween);
      case AttrBetween:
        return onAttrBetween(event);
      case AttrItem:
        return onAttrItem(event, State.AttrBetween);
      case BodyBetween:
        return onBodyBetween(event);
      case BodyExpectingSlot:
        return onBodyExpectingSlot(event);
      case BodyItem:
        return onBodyItem(event);
      default:
        throw new AssertionError(this.state);
    }
  }

  private Recognizer<T> onBodyItem(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.bitSet.set(this.index);
        this.state = State.BodyBetween;
      }
      return this;
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

  private Recognizer<T> onBodyExpectingSlot(ReadEvent event) {
    if (event.isSlot()) {
      this.state = State.BodyItem;
      return this;
    }

    return Recognizer.error(new RuntimeException("Expected a slot event"));
  }

  private Recognizer<T> onBodyBetween(ReadEvent event) {
    if (event.isEndRecord()) {
      try {
        return Recognizer.done(this.builder.bind(), this);
      } catch (RuntimeException e) {
        return Recognizer.error(e);
      }
    } else if (event.isText()) {
      ReadTextValue textValue = (ReadTextValue) event;
      Integer idx = this.indexFn.selectIndex(LabelledFieldKey.item(textValue.getValue()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field \"%s\"", textValue.getValue())));
      } else {
        this.index = idx;
        this.state = State.BodyExpectingSlot;
        return this;
      }
    }

    return Recognizer.error(new RuntimeException("Expected end of record or a text value"));
  }

  private Recognizer<T> onAttrBetween(ReadEvent event) {
    if (event.isStartBody()) {
      this.state = State.BodyBetween;
      return this;
    } else if (event.isStartAttribute()) {
      ReadStartAttribute startAttribute = (ReadStartAttribute) event;
      Integer idx = this.indexFn.selectIndex(LabelledFieldKey.attr(startAttribute.value()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field: \"%s\"", startAttribute.value())));
      } else {
        if (this.bitSet.get(idx)) {
          return Recognizer.error(new RuntimeException(String.format("Duplicate field: \"%s\"", startAttribute.value())));
        } else {
          this.index = idx;
          this.state = State.AttrItem;
          return this;
        }
      }
    }
    return Recognizer.error(new RuntimeException("Expected a record or an attribute"));
  }

  protected void transitionFromInit() {
    Integer idx = this.indexFn.selectIndex(LabelledFieldKey.HEADER);

    if (idx == null) {
      this.state = State.NoHeader;
    } else {
      this.index = idx;
      this.state = State.Header;
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new LabelledClassRecognizer<>(this.tagSpec, this.builder.reset(), this.bitSet.size(), this.indexFn);
  }

  enum State {
    Init,
    Header,
    NoHeader,
    AttrBetween,
    AttrItem,
    BodyBetween,
    BodyExpectingSlot,
    BodyItem,
  }
}
