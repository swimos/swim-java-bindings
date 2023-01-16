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

package ai.swim.codec.input;

import ai.swim.codec.location.Location;

import java.nio.ByteBuffer;

public class ByteBufferInput extends Input {
  private final ByteBuffer data;
  private int offset;
  private final boolean isPartial;

  ByteBufferInput(ByteBuffer data, int offset, boolean isPartial) {
    this.data = data;
    this.offset = offset;
    this.isPartial = isPartial;
  }

  ByteBufferInput(ByteBuffer data) {
    this(data, 0, false);
  }

  @Override
  public boolean has(int n) {
    return false;
  }

  @Override
  public int head() {
    final ByteBuffer buffer = this.data;
    final int position = buffer.position();
    if (position < buffer.limit()) {
      return buffer.get(position) & 0xff;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Input step() {
    final ByteBuffer buffer = this.data;
    final int position = buffer.position();
    if (position < buffer.limit()) {
      buffer.position(position + 1);
      this.offset += 1L;
      return this;
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Location location() {
    return Location.of(0, 0, offset);
  }

  @Override
  public boolean isDone() {
    return !this.isPartial && !this.data.hasRemaining();
  }

  @Override
  public boolean isContinuation() {
    return this.data.hasRemaining();
  }

  @Override
  public boolean isEmpty() {
    return this.isPartial && !this.data.hasRemaining();
  }

  @Override
  public Input setPartial(boolean isPartial) {
    return new ByteBufferInput(data, offset, isPartial);
  }

  @Override
  public void bind(int[] into) {
    throw new AssertionError();
  }

  @Override
  public int len() {
    return this.data.remaining() - this.offset;
  }

  @Override
  public void take(int[] into) {
    throw new AssertionError();
  }

  @Override
  public Input clone() {
    return new ByteBufferInput(data.duplicate(), offset, isPartial);
  }

  @Override
  public Input extend(Input from) {
    throw new AssertionError();
  }
}
