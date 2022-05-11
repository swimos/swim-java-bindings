package ai.swim.structure.recognizer.structural.labelled;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.BitSet;
import ai.swim.structure.recognizer.structural.IndexFn;
import ai.swim.structure.recognizer.structural.key.AttrFieldKey;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class ClassRecognizerAttrBetween<T> extends ClassRecognizer<T> {

  public ClassRecognizerAttrBetween(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    super(tagSpec, builder, bitSet, indexFn, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isStartBody()) {
      return new ClassRecognizerBodyBetween<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
    } else if (event.isStartAttribute()) {
      ReadStartAttribute startAttribute = (ReadStartAttribute) event;
      Integer idx = this.indexFn.selectIndex(new AttrFieldKey(startAttribute.value()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field: \"%s\"", startAttribute.value())));
      } else {
        if (this.bitSet.get(idx)) {
          return Recognizer.error(new RuntimeException(String.format("Duplicate field: \"%s\"", startAttribute.value())));
        } else {
          this.index = idx;
          return new ClassRecognizerAttrItem<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
        }
      }
    }
    return Recognizer.error(new RuntimeException("Expected a record or an attribute"));
  }

  @Override
  public Recognizer<T> reset() {
    return null;
  }

}
