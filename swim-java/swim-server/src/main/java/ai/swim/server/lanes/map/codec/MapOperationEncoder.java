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

package ai.swim.server.lanes.map.codec;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.structure.writer.Writable;

public class MapOperationEncoder<K, V> implements Encoder<MapOperation<K, V>> {
  private final Writable<K> kWriter;
  private final Writable<V> vWriter;

  public MapOperationEncoder(Writable<K> kWriter, Writable<V> vWriter) {
    this.kWriter = kWriter;
    this.vWriter = vWriter;
  }

  @Override
  public void encode(MapOperation<K, V> target, ByteWriter buffer) {
    target.encode(kWriter, vWriter, buffer);
  }

}
