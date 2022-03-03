package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public class ClassRecognizerAttrItem<T> extends ClassRecognizer<T> {
  public ClassRecognizerAttrItem(TagSpec tagSpec, Builder<T> builder, BitSet bitSet, LabelledVTable<T> vTable, int index) {
    super(tagSpec, builder, bitSet, vTable, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    try {
      if (this.vTable.selectRecognize(this.builder, this.index, event)) {
        this.bitSet.set(this.index);
        return new ClassRecognizerAttrBetween<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
      }else {
        return this;
      }
    } catch (Exception e) {
      return Recognizer.error(e);
    }
  }
}
