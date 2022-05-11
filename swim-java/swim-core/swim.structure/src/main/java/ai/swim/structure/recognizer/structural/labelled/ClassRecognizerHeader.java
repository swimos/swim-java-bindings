package ai.swim.structure.recognizer.structural.labelled;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.BitSet;
import ai.swim.structure.recognizer.structural.IndexFn;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

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

  @Override
  public Recognizer<T> reset() {
    return null;
  }

}
