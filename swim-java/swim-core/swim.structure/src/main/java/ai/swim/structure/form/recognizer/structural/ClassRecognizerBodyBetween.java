package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.event.ReadTextValue;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public class ClassRecognizerBodyBetween<T> extends ClassRecognizer<T> {
  public ClassRecognizerBodyBetween(TagSpec tagSpec, Builder<T> builder, BitSet bitSet, LabelledVTable<T> vTable, int index) {
    super(tagSpec, builder, bitSet, vTable, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isEndRecord()) {
      try {
        return Recognizer.done(this.vTable.onDone(this.builder));
      } catch (Exception e) {
        return Recognizer.error(e);
      }
    } else if (event.isText()) {
      ReadTextValue textValue = (ReadTextValue) event;
      Integer idx = this.vTable.selectIndex(new ItemFieldKey(textValue.value()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field \"%s\"", textValue.value())));
      } else {
        this.index = idx;
        return new ClassRecognizerBodyExpectingSlot<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
      }
    }

    return Recognizer.error(new RuntimeException("Expected end of record or a text value"));
  }
}
