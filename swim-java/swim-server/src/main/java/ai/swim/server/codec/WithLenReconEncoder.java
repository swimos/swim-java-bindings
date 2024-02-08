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

package ai.swim.server.codec;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.codec.encoder.EncoderException;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.print.StructurePrinter;
import ai.swim.structure.writer.print.strategy.PrintStrategy;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * An encoder that a {@link Writable} into {@link ByteWriter} and prefixes it by the length of the produced number of bytes.
 */
public class WithLenReconEncoder<V, T extends Writable<V>> implements Encoder<V> {
  private final T writable;

  public WithLenReconEncoder(T writable) {
    this.writable = writable;
  }

  @Override
  public void encode(V target, ByteWriter dst) {
    int startIdx = dst.writePosition();
    dst.writeLong(0);
    int startLen = dst.writePosition();

    OutputStreamWriter writer = new OutputStreamWriter(dst.outputStream(), StandardCharsets.UTF_8);
    writable.writeInto(target, new StructurePrinter(writer, PrintStrategy.COMPACT));

    try {
      writer.flush();
    } catch (IOException e) {
      throw new EncoderException(e);
    }

    dst.writeLong(dst.writePosition() - startLen, startIdx);
  }
}
