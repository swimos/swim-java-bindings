// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.bridge.buffer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HeapByteBuffer implements Buffer {
  // todo: It's a bit overkill using a VarHandle here but it's the simplest way to perform atomic operations on a byte
  //  buffer outside of using Unsafe directly and lowers the amount of overhead in writing a JNI buffer.
  private static final VarHandle BUFFER_VH = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());
  private final ByteBuffer buffer;

  /**
   * Creates a new heap allocated byte buffer.
   *
   * @param capacity in bytes
   */
  public HeapByteBuffer(int capacity) {
    this.buffer = ByteBuffer.allocateDirect(capacity);
  }

  public ByteBuffer rawBuffer() {
    return this.buffer;
  }

  @Override
  public int getIntVolatile(int idx) {
    return ((int) BUFFER_VH.getVolatile(buffer, idx));
  }

  @Override
  public int getInt(int idx) {
    return (int) BUFFER_VH.get(buffer, idx);
  }

  @Override
  public void setIntVolatile(int idx, int to) {
    BUFFER_VH.setVolatile(buffer, idx, to);
  }

  @Override
  public void setInt(int idx, int to) {
    BUFFER_VH.set(buffer, idx, to);
  }

  @Override
  public byte getByte(int idx) {
    return buffer.get(idx);
  }

  @Override
  public void setByte(int idx, byte to) {
    buffer.put(idx, to);
  }

  @Override
  public int capacity() {
    return buffer.capacity();
  }

}
