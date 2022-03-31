package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class ClassRecognizerBodyItem<T> extends ClassRecognizer<T> {

  public ClassRecognizerBodyItem(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    super(tagSpec, builder, bitSet, indexFn, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    try {
      if (this.builder.feedIndexed(this.index, event)) {
        this.bitSet.set(this.index);
        return new ClassRecognizerBodyBetween<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
      } else {
        return this;
      }
    } catch (RuntimeException e) {
      return Recognizer.error(e);
    }
  }

}
