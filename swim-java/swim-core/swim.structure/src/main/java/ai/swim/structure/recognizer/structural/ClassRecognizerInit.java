package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.key.HeaderFieldKey;
import ai.swim.structure.recognizer.structural.key.LabelledFieldKey;
import ai.swim.structure.recognizer.structural.key.TagFieldKey;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class ClassRecognizerInit<T> extends ClassRecognizer<T> {

  public ClassRecognizerInit(TagSpec tagSpec, RecognizingBuilder<T> builder, int fieldCount, IndexFn indexFn) {
    super(tagSpec, builder, fieldCount, indexFn);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isStartAttribute()) {
      ReadStartAttribute attributeEvent = (ReadStartAttribute) event;

      if (this.tagSpec.isFixed()) {
        FixedTagSpec fixedTag = (FixedTagSpec) this.tagSpec;

        if (fixedTag.getTag().equals(attributeEvent.value())) {
          return this.nextState(new HeaderFieldKey());
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
              return this.nextState(new HeaderFieldKey());
            } else {
              return this;
            }
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

  @Override
  public Recognizer<T> reset() {
    return null;
  }

  private Recognizer<T> nextState(LabelledFieldKey key) {
    Integer idx = this.indexFn.selectIndex(key);

    if (idx == null) {
      return new ClassRecognizerNoHeader<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
    } else {
      this.index = idx;
      return new ClassRecognizerHeader<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
    }
  }

}
