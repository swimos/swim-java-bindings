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

package ai.swim.server.lanes.value;

import ai.swim.codec.data.BufferOverflowException;
import ai.swim.codec.data.ByteWriter;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.codec.WithLenReconEncoder;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponseEncoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.state.State;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class ValueState<T> implements State {
  private final Form<T> form;
  private final StateCollector collector;
  private final List<T> events;
  private final List<UUID> syncRequests;
  private final int laneId;
  private T state;
  private boolean dirty;

  public ValueState(int laneId, Form<T> form, StateCollector collector) {
    this.laneId = laneId;
    this.form = form;
    this.collector = collector;
    events = new ArrayList<>();
    syncRequests = new ArrayList<>();
  }

  /**
   * Sets the current state.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public T set(T to) {
    CallContext.check();

    T oldState = state;
    dirty = true;
    state = to;
    events.add(to);
    collector.add(this);
    return oldState;
  }

  /**
   * Gets the current state.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public T get() {
    CallContext.check();
    return state;
  }

  private void write(ByteWriter buffer, LaneResponse<T> item) {
    IdentifiedLaneResponseEncoder<T> encoder = new IdentifiedLaneResponseEncoder<>(new WithLenReconEncoder<>(form));
    encoder.encode(new IdentifiedLaneResponse<>(laneId, item), buffer);
  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
    ListIterator<UUID> syncIter = syncRequests.listIterator();

    while (syncIter.hasNext()) {
      UUID remote = syncIter.next();

      try {
        write(bytes, LaneResponse.syncEvent(remote, state));
        write(bytes, LaneResponse.synced(remote));
      } catch (BufferOverflowException ignored) {
        return WriteResult.DataStillAvailable;
      }

      syncIter.remove();
    }

    if (dirty) {
      ListIterator<T> iter = events.listIterator();

      while (iter.hasNext()) {
        T item = iter.next();

        try {
          write(bytes, LaneResponse.event(item));
        } catch (BufferOverflowException ignored) {
          return WriteResult.DataStillAvailable;
        }

        iter.remove();
      }

      dirty = false;
      return WriteResult.Done;
    } else {
      return WriteResult.NoData;
    }
  }

  @Override
  public void sync(UUID uuid) {
    syncRequests.add(uuid);
    collector.add(this);
  }

}
