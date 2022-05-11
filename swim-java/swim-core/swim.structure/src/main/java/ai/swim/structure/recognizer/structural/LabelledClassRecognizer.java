package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.key.AttrFieldKey;
import ai.swim.structure.recognizer.structural.key.HeaderFieldKey;
import ai.swim.structure.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.recognizer.structural.key.TagFieldKey;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class LabelledClassRecognizer<T> extends Recognizer<T> {

  private final TagSpec tagSpec;
  private final RecognizingBuilder<T> builder;
  private final BitSet bitSet;
  private final IndexFn indexFn;
  private int index;
  private State state;

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

  public LabelledClassRecognizer(TagSpec tagSpec, RecognizingBuilder<T> builder, int fieldCount, IndexFn indexFn) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = new BitSet(fieldCount);
    this.indexFn = indexFn;
    this.index = 0;
    this.state = State.Init;
  }

  public LabelledClassRecognizer(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = bitSet;
    this.indexFn = indexFn;
    this.index = index;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        return onInit(event);
      case Header:
        return onHeader(event);
      case NoHeader:
        return onNoHeader(event);
      case AttrBetween:
        return onAttrBetween(event);
      case AttrItem:
        return onAttrItem(event);
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

  private Recognizer<T> onNoHeader(ReadEvent event) {
    if (event.isExtant()) {
      return this;
    } else if (event.isEndAttribute()) {
      this.state = State.AttrBetween;
      return this;
    } else {
      return Recognizer.error(new RuntimeException("Expected the end of an attribute"));
    }
  }

  private Recognizer<T> onHeader(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.state = State.AttrBetween;
      }
      return this;
    } catch (RuntimeException e) {
      return Recognizer.error(e);
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
      Integer idx = this.indexFn.selectIndex(new ItemFieldKey(textValue.value()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field \"%s\"", textValue.value())));
      } else {
        this.index = idx;
        this.state = State.BodyExpectingSlot;
        return this;
      }
    }

    return Recognizer.error(new RuntimeException("Expected end of record or a text value"));
  }

  private Recognizer<T> onAttrItem(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.bitSet.set(this.index);
        this.state = State.AttrBetween;
      }
      return this;
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

  private Recognizer<T> onAttrBetween(ReadEvent event) {
    if (event.isStartBody()) {
      this.state = State.BodyBetween;
      return this;
    } else if (event.isStartAttribute()) {
      ReadStartAttribute startAttribute = (ReadStartAttribute) event;
      Integer idx = this.indexFn.selectIndex(new AttrFieldKey(startAttribute.value()));

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

  private Recognizer<T> onInit(ReadEvent event) {
    if (event.isStartAttribute()) {
      ReadStartAttribute attributeEvent = (ReadStartAttribute) event;

      if (this.tagSpec.isFixed()) {
        FixedTagSpec fixedTag = (FixedTagSpec) this.tagSpec;

        if (fixedTag.getTag().equals(attributeEvent.value())) {
          this.transitionFromInit();
          return this;
        } else {
          return Recognizer.error(new RuntimeException("Unexpected attribute: " + attributeEvent.value()));
        }
      } else if (this.tagSpec.isField()) {
        Integer idx = this.indexFn.selectIndex(new TagFieldKey());

        if (idx == null) {
          return Recognizer.error(new RuntimeException("Inconsistent state"));
        } else {
          try {
            if (this.builder.feedIndexed(idx, new ReadTextValue(attributeEvent.value()))) {
              this.transitionFromInit();
            }
            return this;
          } catch (RuntimeException e) {
            return Recognizer.error(e);
          }
        }
      } else {
        throw new AssertionError();
      }
    }

    if (this.tagSpec.isFixed()) {
      FixedTagSpec fixedTag = (FixedTagSpec) this.tagSpec;
      return Recognizer.error(new RuntimeException(String.format("Expected an attribute with a name of \"%s\"", fixedTag.getTag())));
    } else {
      return Recognizer.error(new RuntimeException("Expected an attribute"));
    }
  }

  private void transitionFromInit() {
    Integer idx = this.indexFn.selectIndex(new HeaderFieldKey());

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
}
