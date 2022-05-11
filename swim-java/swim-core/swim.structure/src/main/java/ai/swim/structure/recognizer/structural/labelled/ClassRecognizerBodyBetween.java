package ai.swim.structure.recognizer.structural.labelled;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.BitSet;
import ai.swim.structure.recognizer.structural.IndexFn;
import ai.swim.structure.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public class ClassRecognizerBodyBetween<T> extends ClassRecognizer<T> {

  public ClassRecognizerBodyBetween(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    super(tagSpec, builder, bitSet, indexFn, index);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isEndRecord()) {
      try {
        return Recognizer.done(this.builder.bind(), this);
      } catch (RuntimeException e) {
        return Recognizer.error(e);
      }
    } else if (event.isText()) {
      ReadTextValue textValue = (ReadTextValue) event;
      Integer idx = this.indexFn.selectIndex(new ItemFieldKey(textValue.value()));

      if (idx == null) {
        return Recognizer.error(new RuntimeException(String.format("Unexpected field \"%s\"", textValue.value())));
      } else {
        this.index = idx;
        return new ClassRecognizerBodyExpectingSlot<>(this.tagSpec, this.builder, this.bitSet, this.indexFn, this.index);
      }
    }

    return Recognizer.error(new RuntimeException("Expected end of record or a text value"));
  }

  @Override
  public Recognizer<T> reset() {
    return null;
  }

}
