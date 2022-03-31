package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class ClassRecognizerNoHeader<T> extends ClassRecognizer<T> {

  public ClassRecognizerNoHeader(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    super(tagSpec, builder, bitSet, indexFn, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isExtant()) {
      return this;
    } else if (event.isEndAttribute()) {
      return new ClassRecognizerAttrBetween<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
    } else {
      return Recognizer.error(new RuntimeException("Expected the end of an attribute"));
    }
  }

}
