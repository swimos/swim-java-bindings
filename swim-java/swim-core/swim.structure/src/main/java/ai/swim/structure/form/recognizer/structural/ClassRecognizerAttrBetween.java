package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.event.ReadStartAttribute;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.key.AttrFieldKey;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public class ClassRecognizerAttrBetween<T> extends ClassRecognizer<T> {
  public ClassRecognizerAttrBetween(TagSpec tagSpec, Builder<T> builder, BitSet bitSet, LabelledVTable<T> vTable, int index) {
    super(tagSpec, builder, bitSet, vTable, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isStartBody()) {
      return new ClassRecognizerBodyBetween<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
    } else if (event.isStartAttribute()) {
      ReadStartAttribute startAttribute = (ReadStartAttribute) event;
      Integer idx = this.vTable.selectIndex(new AttrFieldKey(startAttribute.value()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field: \"%s\"", startAttribute.value())));
      } else {
        if (this.bitSet.get(idx)) {
          return Recognizer.error(new RuntimeException(String.format("Duplicate field: \"%s\"", startAttribute.value())));
        } else {
          this.index = idx;
          return new ClassRecognizerAttrItem<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
        }
      }
    }
    return Recognizer.error(new RuntimeException("Expected a record or an attribute"));
  }
}
