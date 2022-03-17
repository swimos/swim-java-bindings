package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.RecognizingBuilder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public class ClassRecognizerHeader<T> extends ClassRecognizer<T> {

  public ClassRecognizerHeader(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    super(tagSpec, builder, bitSet, indexFn, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        return new ClassRecognizerAttrBetween<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
      } else {
        return this;
      }
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

}