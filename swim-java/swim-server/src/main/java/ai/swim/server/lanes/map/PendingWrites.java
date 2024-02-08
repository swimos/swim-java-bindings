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
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponseEncoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.structure.writer.Writable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stack of pending writes that need to be written.
 *
 * @param <K> map lane's key type.
 * @param <V> map lane's value type.
 */
public class PendingWrites<K, V> {
  private final Deque<MapSyncRequest<K>> syncQueue;
  private final Deque<MapOperation<K, V>> operationQueue;
  private Bias bias;

  private enum Bias {
    Sync, Event
  }

  public PendingWrites() {
    syncQueue = new ArrayDeque<>();
    operationQueue = new ArrayDeque<>();
    bias = Bias.Sync;
  }

  /**
   * Pushes a new sync request.
   *
   * @param remote UUID.
   * @param keys   that were contained in the map's state.
   */
  public void pushSync(UUID remote, Set<K> keys) {
    syncQueue.addLast(new MapSyncRequest<>(remote, keys));
  }

  /**
   * Pushes a new map operation.
   *
   * @param operation to add.
   */
  public void pushOperation(MapOperation<K, V> operation) {
    operationQueue.addLast(operation);
  }

  /**
   * Writes any pending events into {@code byteWriter}.
   * <p>
   * Sync events are interleaved with any map operations.
   *
   * @param laneId     the ID of the map lane.
   * @param mapLookup      the map's current state.
   * @param byteWriter to write the events into.
   * @param keyForm    for writing the map's key type.
   * @param valueForm  for writing the map's value type.
   * @return the result of the operation.
   */
  public WriteResult writeInto(int laneId,
      MapLookup<K, V> mapLookup,
      ByteWriter byteWriter,
      Writable<K> keyForm,
      Writable<V> valueForm) {
    if (syncQueue.isEmpty() && operationQueue.isEmpty()) {
      return WriteResult.NoData;
    }

    boolean syncComplete = false;

    while (true) {
      switch (bias) {
        case Sync:
          MapSyncRequest<K> syncRequest = syncQueue.peek();
          if (syncRequest != null) {
            try {
              if (syncRequest.encodeInto(laneId, byteWriter, keyForm, valueForm, mapLookup)) {
                syncQueue.poll();
              }
            } catch (BufferOverflowException ignored) {
              return WriteResult.DataStillAvailable;
            }
          } else {
            syncComplete = true;
          }
          bias = Bias.Event;
          break;
        case Event:
          MapOperation<K, V> event = operationQueue.peek();
          if (event != null) {
            try {
              IdentifiedLaneResponseEncoder<MapOperation<K, V>> encoder = new IdentifiedLaneResponseEncoder<>(new MapOperationEncoder<>(
                  keyForm,
                  valueForm));
              encoder.encode(new IdentifiedLaneResponse<>(laneId, LaneResponse.event(event)), byteWriter);
              operationQueue.poll();
            } catch (BufferOverflowException ignored) {
              return WriteResult.DataStillAvailable;
            }
          } else if (syncComplete) {
            return WriteResult.Done;
          }

          if (!syncComplete) {
            bias = Bias.Sync;
          }

          break;
        default:
          throw new AssertionError("Unhandled bias: " + bias);
      }
    }
  }

}
