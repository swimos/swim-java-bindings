package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public class ClassRecognizerNoHeader<T> extends ClassRecognizer<T> {
  public ClassRecognizerNoHeader(TagSpec tagSpec, Builder<T> builder, BitSet bitSet, LabelledVTable<T> vTable, int index) {
    super(tagSpec, builder, bitSet, vTable, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isExtant()) {
      return this;
    } else if (event.isEndAttribute()) {
      return new ClassRecognizerAttrBetween<>(this.tagSpec, this.builder, this.bitSet, this.vTable, this.index);
    } else {
      return Recognizer.error(new RuntimeException("Expected the end of an attribute"));
    }
  }
}
