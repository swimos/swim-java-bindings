package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.event.ReadStartAttribute;
import ai.swim.structure.form.event.ReadTextValue;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.key.HeaderFieldKey;
import ai.swim.structure.form.recognizer.structural.key.LabelledFieldKey;
import ai.swim.structure.form.recognizer.structural.key.TagFieldKey;
import ai.swim.structure.form.recognizer.structural.tag.FixedTagSpec;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public class ClassRecognizerInit<T> extends ClassRecognizer<T> {
  public ClassRecognizerInit(TagSpec tagSpec, Builder<T> builder, int fieldCount, LabelledVTable<T> vTable) {
    super(tagSpec, builder, fieldCount, vTable);
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
        Integer idx = this.vTable.selectIndex(new TagFieldKey());

        if (idx == null) {
          return Recognizer.error(new RuntimeException("Inconsistent state"));
        } else {
          try {
            if (this.vTable.selectRecognize(this.builder, idx, new ReadTextValue(attributeEvent.value()))) {
              return this.nextState(new HeaderFieldKey());
            } else {
              return this;
            }
          } catch (Exception e) {
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

  private Recognizer<T> nextState(LabelledFieldKey key) {
    Integer idx = this.vTable.selectIndex(key);

    if (idx == null) {
      return new ClassRecognizerNoHeader<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
    } else {
      this.index = idx;
      return new ClassRecognizerHeader<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
    }
  }
}
