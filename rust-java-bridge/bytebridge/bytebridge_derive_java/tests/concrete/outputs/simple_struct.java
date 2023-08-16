/// THIS FILE IS AUTOMATICALLY GENERATED BY THE BYTE BRIDGE LIBRARY.
/// ANY CHANGES MADE MAY BE LOST.
package ai.swim;

import org.msgpack.core.MessagePacker;
import java.io.IOException;

public class Test {

  private int a = 0;

  private int b = 0;

  /**
   * Gets a.
   * <p>
   * Default value: 0.
   *
   * @return a
   */
  public int getA() {
    return this.a;
  }

  /**
   * Sets the new a.
   *
   * @param a the new a
   */
  public void setA(int a) {
    this.a = a;
  }

  /**
   * Gets b.
   * <p>
   * Default value: 0.
   *
   * @return b
   */
  public int getB() {
    return this.b;
  }

  /**
   * Sets the new b.
   *
   * @param b the new b
   */
  public void setB(int b) {
    this.b = b;
  }

  /**
   * Returns a byte array representation of the current configuration.
   */
  public void pack(MessagePacker __packer) throws IOException {
    __packer.packArrayHeader(2);
    __packer.packInt(this.a);
    __packer.packInt(this.b);
  }

  @Override
  public String toString() {
    return "Test{" +
     "a='" + a + '\'' +
     ", b='" + b + '\'' +
     '}';
  }

}
