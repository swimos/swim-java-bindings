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

package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import java.nio.charset.StandardCharsets;

public class AgentFixture {

  public static void writeIntString(int v, ByteWriter into) {
    byte[] bytes = Integer.toString(v).getBytes(StandardCharsets.UTF_8);
    into.writeLong(bytes.length);
    into.writeByteArray(bytes);
  }

  public static void writeBooleanString(Boolean v, ByteWriter into) {
    byte[] bytes = Boolean.toString(v).getBytes(StandardCharsets.UTF_8);
    into.writeLong(bytes.length);
    into.writeByteArray(bytes);
  }

  public static <E> ByteWriter encodeIter(Iterable<E> iterator, Encoder<E> encoder) {
    ByteWriter writer = new ByteWriter();
    encodeIter(writer, iterator, encoder);
    return writer;
  }

  public static <E> void encodeIter(ByteWriter writer, Iterable<E> iterator, Encoder<E> encoder) {
    for (E e : iterator) {
      encoder.encode(e, writer);
    }
  }

}
