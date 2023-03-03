package ai.swim.structure.recognizer.structural;

public class BitSet {
  private final int cap;
  private long bits;

  public BitSet(int cap) {
    this.cap = cap;
    this.bits = 0;
  }

  public void set(int index) {
    if (index <= cap) {
      this.bits |= 1L << index;
    }
  }

  public boolean get(int index) {
    if (index <= this.cap) {
      return ((this.bits >> index) & 0x1) != 0;
    } else {
      return false;
    }
  }

  public int size() {
    return this.cap;
  }

}
