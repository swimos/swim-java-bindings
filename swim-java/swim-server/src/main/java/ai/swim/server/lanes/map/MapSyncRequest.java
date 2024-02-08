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

package ai.swim.server.lanes.map;

import ai.swim.codec.data.BufferOverflowException;
import ai.swim.codec.data.ByteWriter;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponseEncoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.structure.writer.Writable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MapSyncRequest<K> {
  private final UUID remote;
  private final Set<K> keys;

  public MapSyncRequest(UUID remote, Set<K> keys) {
    this.remote = remote;
    this.keys = keys;
  }

  public <V> boolean encodeInto(int laneId,
      ByteWriter byteWriter,
      Writable<K> keyForm,
      Writable<V> valueForm,
      MapLookup<K, V> mapLookup) throws BufferOverflowException {
    IdentifiedLaneResponseEncoder<MapOperation<K, V>> encoder = new IdentifiedLaneResponseEncoder<>(new MapOperationEncoder<>(
        keyForm,
        valueForm));
    Iterator<K> keyIter = keys.iterator();

    if (keyIter.hasNext()) {
      while (keyIter.hasNext()) {
        K key = keyIter.next();
        V value = mapLookup.get(key);

        if (value != null) {
          encoder.encode(
              new IdentifiedLaneResponse<>(laneId, LaneResponse.syncEvent(remote, MapOperation.update(key, value))),
              byteWriter);
          keyIter.remove();
          return false;
        }
      }
    } else {
      encoder.encode(new IdentifiedLaneResponse<>(laneId, LaneResponse.synced(remote)), byteWriter);
    }
    return true;
  }

  @Override
  public String toString() {
    return "MapSyncRequest{" +
        "remote=" + remote +
        ", keys=" + keys +
        '}';
  }
}
