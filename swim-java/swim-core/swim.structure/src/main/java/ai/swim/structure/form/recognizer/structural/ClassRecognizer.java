package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.structural.tag.TagSpec;

public abstract class ClassRecognizer<T> extends Recognizer<T> {

  protected final TagSpec tagSpec;
  protected final Builder<T> builder;
  protected final BitSet bitSet;
  protected final LabelledVTable<T> vTable;
  protected int index;

  protected ClassRecognizer(TagSpec tagSpec, Builder<T> builder, int fieldCount, LabelledVTable<T> vTable) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = new BitSet(fieldCount);
    this.vTable = vTable;
    this.index = 0;
  }

  protected ClassRecognizer(TagSpec tagSpec, Builder<T> builder, BitSet bitSet, LabelledVTable<T> vTable, int index) {
    this.tagSpec = tagSpec;
    this.builder = builder;
    this.bitSet = bitSet;
    this.vTable = vTable;
    this.index = index;
  }

  public static <T> ClassRecognizer<T> init(TagSpec tagSpec, Builder<T> builder, int fieldCount, LabelledVTable<T> vTable) {
    return new ClassRecognizerInit<>(tagSpec, builder, fieldCount, vTable);
  }

}
