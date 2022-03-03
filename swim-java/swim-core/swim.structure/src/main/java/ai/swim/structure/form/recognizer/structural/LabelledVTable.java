package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.structural.key.LabelledFieldKey;

public class LabelledVTable<T> {
  private final IndexFn indexFn;
  private final RecogFn<T> recogFn;
  private final DoneFn<T> doneFn;
  private final ResetFn<T> resetFn;

  public LabelledVTable(IndexFn indexFn, RecogFn<T> recogFn, DoneFn<T> doneFn, ResetFn<T> resetFn) {
    this.indexFn = indexFn;
    this.recogFn = recogFn;
    this.doneFn = doneFn;
    this.resetFn = resetFn;
  }

  public Integer selectIndex(LabelledFieldKey key) {
    return this.indexFn.selectIndex(key);
  }

  public boolean selectRecognize(Builder<T> builder, int index, ReadEvent event) {
    return this.recogFn.selectRecognize(builder, index, event);
  }

  public T onDone(Builder<T> builder) {
    return this.doneFn.onDone(builder);
  }

  public void reset(Builder<T> builder) {
    this.resetFn.reset(builder);
  }
}