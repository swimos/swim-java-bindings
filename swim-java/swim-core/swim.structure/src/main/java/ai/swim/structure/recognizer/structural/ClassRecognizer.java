package ai.swim.structure.recognizer.structural;

import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.tag.TagSpec;

public abstract class ClassRecognizer<T> extends Recognizer<T> {

  protected final TagSpec tagSpec;
  protected final RecognizingBuilder<T> builder;
  protected final BitSet bitSet;
  protected final IndexFn indexFn;
  protected int index;

  protected ClassRecognizer(TagSpec tagSpec, RecognizingBuilder<T> builder, int fieldCount, IndexFn indexFn) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = new BitSet(fieldCount);
    this.indexFn = indexFn;
    this.index = 0;
  }

  protected ClassRecognizer(TagSpec tagSpec, RecognizingBuilder<T> builder, BitSet bitSet, IndexFn indexFn, int index) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = bitSet;
    this.indexFn = indexFn;
    this.index = index;
  }

  public static <T> ClassRecognizer<T> init(TagSpec tagSpec, RecognizingBuilder<T> builder, int fieldCount, IndexFn indexFn) {
    return new ClassRecognizerInit<>(tagSpec, builder, fieldCount, indexFn);
  }

}
