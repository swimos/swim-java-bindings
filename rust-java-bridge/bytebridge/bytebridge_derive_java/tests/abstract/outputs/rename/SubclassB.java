/// THIS FILE IS AUTOMATICALLY GENERATED BY THE BYTE BRIDGE LIBRARY.
/// ANY CHANGES MADE MAY BE LOST.
package ai.swim;

public class SubclassB extends Superclass {

  private int c = 0;

  private String d = "";

  /**
   * Gets c.
   * <p>
   * Default value: 0.
   *
   * @return c
   */
  public int getC() {
    return this.c;
  }

  /**
   * Sets the new c.
   *
   * @param c the new c
   * @throws IllegalArgumentException if c is negative
   */
  public void setC(int c) {
    if (c < 0) {
      throw new IllegalArgumentException("'c' must be positive");
    }
    this.c = c;
  }

  /**
   * Gets d.
   * <p>
   * Default value: "".
   *
   * @return d
   */
  public String getD() {
    return this.d;
  }

  /**
   * Sets the new d.
   *
   * @param d the new d
   */
  public void setD(String d) {
    this.d = d;
  }

  /**
   * Returns a byte array representation of the current configuration.
   */
  @Override
  public byte[] asBytes() {
    int __buf__size = 0;
    __buf__size += d.length();
    __buf__size += 5;
    java.nio.ByteBuffer __buf = java.nio.ByteBuffer.allocate(__buf__size);
    __buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
    __buf.put((byte) 1);
    __buf.putInt(this.c);
    __buf.putInt(this.d.length());
    __buf.put(this.d.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return __buf.array();
  }

  @Override
  public String toString() {
    return "SubclassB{" +
     "c='" + c + '\'' +
     ", d='" + d + '\'' +
     '}';
  }

}
