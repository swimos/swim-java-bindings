// Copyright 2015-2021 Swim Inc.
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

package ai.swim.codec.data;

import java.util.Arrays;

public class ByteArrayOutput {

  private byte[] data;
  int size;

  public ByteArrayOutput(int initialSize) {
    this.data = new byte[initialSize];
    this.size = 0;
  }

  public ByteArrayOutput push(int b) {
    final int n = this.size;
    final byte[] oldArray = this.data;
    final byte[] newArray;
    if (oldArray == null || n + 1 > oldArray.length) {
      newArray = new byte[expand(n + 1)];

      if (oldArray != null) {
        System.arraycopy(oldArray, 0, newArray, 0, n);
      }

      this.data = newArray;
    } else {
      newArray = oldArray;
    }

    newArray[n] = (byte) b;
    this.size = n + 1;

    return this;
  }

  public byte[] getData() {
    return Arrays.copyOfRange(this.data, 0, this.size);
  }

  static int expand(int n) {
    n = Math.max(32, n) - 1;
    n |= n >> 1;
    n |= n >> 2;
    n |= n >> 4;
    n |= n >> 8;
    n |= n >> 16;
    return n + 1;
  }
}
