package ai.swim.structure.recognizer.structural.labelled;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.BitSet;
import ai.swim.structure.recognizer.structural.IndexFn;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class ClassRecognizerBodyExpectingSlot<T> extends ClassRecognizer<T> {

  public ClassRecognizerBodyExpectingSlot(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    super(tagSpec, builder, bitSet, indexFn, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isSlot()) {
      return new ClassRecognizerBodyItem<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
    }

    return Recognizer.error(new RuntimeException("Expected a slot event"));
  }

  @Override
  public Recognizer<T> reset() {
    return null;
  }

}
