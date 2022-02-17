package ai.swim.sync;

public enum Ordering {

  Relaxed(0),
  Release(1),
  Acquire(2),
  AcqRel(3),
  SeqCst(4);

  private final int ordinal;

  Ordering(int ordinal) {
    this.ordinal = ordinal;
  }

  public int getOrdinal() {
    return ordinal;
  }

}
