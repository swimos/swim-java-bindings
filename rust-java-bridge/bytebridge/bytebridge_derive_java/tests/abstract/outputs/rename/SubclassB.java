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
  public void pack(MessagePacker __packer) throws IOException {
    __packer.packExtensionTypeHeader((byte) 1, 1);
    __packer.packInt(1);
    __packer.packArrayHeader(2);
    __packer.packInt(this.c);
    __packer.packString(this.d);
  }

  @Override
  public String toString() {
    return "SubclassB{" +
     "c='" + c + '\'' +
     ", d='" + d + '\'' +
     '}';
  }

}
