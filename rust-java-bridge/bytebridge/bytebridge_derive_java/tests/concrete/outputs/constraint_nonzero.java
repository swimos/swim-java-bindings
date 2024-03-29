// Copyright 2015-2024 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
   * @throws IllegalArgumentException if a is zero
   */
  public void setA(int a) {
    if (a == 0) {
      throw new IllegalArgumentException("'a' must be non-zero");
    }
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
