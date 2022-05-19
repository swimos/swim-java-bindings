package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public abstract class ClassRecognizer<State, Key, T> extends Recognizer<T> {
  protected final TagSpec tagSpec;
  protected final RecognizingBuilder<T> builder;
  protected final BitSet bitSet;
  protected final IndexFn<Key> indexFn;
  protected int index;
  protected State state;

  protected ClassRecognizer(TagSpec tagSpec, RecognizingBuilder<T> builder, int fieldCount, IndexFn<Key> indexFn, State state) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = new BitSet(fieldCount);
    this.indexFn = indexFn;
    this.index = 0;
    this.state = state;
  }

  protected Recognizer<T> onInit(ReadEvent event, Key key) {
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
        Integer idx = this.indexFn.selectIndex(key);

        if (idx == null) {
          return Recognizer.error(new RuntimeException("Inconsistent state"));
        } else {
          try {
            if (this.builder.feedIndexed(idx, ReadEvent.text(attributeEvent.value()))) {
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

  protected abstract void transitionFromInit();

  protected Recognizer<T> onHeader(ReadEvent event, State state) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.state = state;
      }
      return this;
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

  protected Recognizer<T> onNoHeader(ReadEvent event, State state) {
    if (event.isExtant()) {
      return this;
    } else if (event.isEndAttribute()) {
      this.state = state;
      return this;
    } else {
      return Recognizer.error(new RuntimeException("Expected the end of an attribute"));
    }
  }

  protected Recognizer<T> onAttrItem(ReadEvent event, State state) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.bitSet.set(this.index);
        this.state = state;
      }
      return this;
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }
}
