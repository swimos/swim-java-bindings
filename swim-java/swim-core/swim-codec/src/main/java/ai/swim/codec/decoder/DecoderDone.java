/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.codec.decoder;


import ai.swim.codec.data.ReadBuffer;

/**
 * A decoder in a done state.
 *
 * @param <T> decoder's target type.
 */
public class DecoderDone<T> extends Decoder<T> {
  private final Decoder<T> decoder;
  private final T value;

  public DecoderDone(Decoder<T> decoder, T value) {
    this.decoder = decoder;
    this.value = value;
  }

  @Override
  public Decoder<T> decode(ReadBuffer buffer) {
    throw new IllegalStateException("Decoder complete");
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public T bind() {
    return value;
  }

  @Override
  public Decoder<T> reset() {
    return decoder.reset();
  }

}
