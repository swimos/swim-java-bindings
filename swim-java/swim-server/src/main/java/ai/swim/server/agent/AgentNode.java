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

import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.agent.task.TaskRegistry;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.state.StateCollector;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Agent node definition containing an agent-scoped state collector for tracking state changes across the registered
 * lanes and the lanes themselves.
 */
public class AgentNode {
  /**
   * A collector for managing which lanes have become dirty since the delivery of an envelope to a lane. A single
   * envelope may cause {@code N} events to fire across the agent and this keeps track of which lanes need their state
   * flushing back to the Rust runtime.
   */
  private final StateCollector collector;
  /**
   * A map containing a key that is a unique lane identifier that has been registered with the Rust runtime and a value
   * that is an initialised {@link LaneModel} that has been registered with the {@link StateCollector} below.
   */
  private final Map<Integer, LaneModel> lanes;
  /**
   * Mapping from laneUri -> laneId.
   */
  private final Map<String, Integer> laneMappings;
  private final TaskRegistry taskRegistry;
  private AgentState state;

  public AgentNode(StateCollector collector, Map<Integer, LaneModel> lanes, Map<String, Integer> laneMappings) {
    this.collector = collector;
    this.lanes = lanes;
    this.laneMappings = laneMappings;
    taskRegistry = new TaskRegistry();
    state = AgentState.NotStarted;
  }

  /**
   * Dispatch an event to {@code laneIdx}
   *
   * @param laneIdx the URI of the lane.
   * @param buffer  the event data.
   * @return an array containing encoded {@link ai.swim.server.lanes.models.response.LaneResponse}s.
   */
  public byte[] dispatch(int laneIdx, ByteBuffer buffer) throws DecoderException {
    CallContext.enter();

    lanes.get(laneIdx).dispatch(ReadBuffer.byteBuffer(buffer));
    byte[] bytes = flushState();

    CallContext.exit();
    return bytes;
  }

  /**
   * Dispatch a sync request to {@code laneIdx} that was requested by a remote.
   *
   * @param laneIdx the URI of the lane.
   * @param uuidMsb UUID most significant bits.
   * @param uuidLsb UUID least significant bits.
   * @return an array containing an encoded {@link ai.swim.server.lanes.models.response.LaneResponse} sync.
   */
  public byte[] sync(int laneIdx, long uuidMsb, long uuidLsb) throws DecoderException {
    lanes.get(laneIdx).sync(new UUID(uuidMsb, uuidLsb));
    return flushState();
  }

  /**
   * Initialise {@code laneIdx} with the store data in {@code from}.
   *
   * @param laneIdx to initialise.
   * @param from    the store initialisation data.
   */
  public void init(int laneIdx, ByteBuffer from) throws DecoderException {
    lanes.get(laneIdx).init(ReadBuffer.byteBuffer(from));
  }

  public int nextLaneId() {
    return Collections.max(laneMappings.values()) + 1;
  }

  public void addLane(String laneUri, int id, LaneModel laneModel) {
    laneMappings.put(laneUri, id);
    lanes.put(id, laneModel);
  }

  /**
   * Returns whether this node contains a lane of {@code laneUri}.
   */
  public boolean containsLane(String laneUri) {
    return laneMappings.containsKey(laneUri);
  }

  /**
   * Flush any pending state from the {@link StateCollector}.
   *
   * @return an array containing encoded {@link ai.swim.server.lanes.models.response.LaneResponse}s.
   */
  public byte[] flushState() {
    CallContext.enter();
    byte[] state = collector.flushState();
    CallContext.exit();

    return state;
  }

  /**
   * Returns the {@link StateCollector} associated with this {@link AgentNode}.
   */
  public StateCollector getCollector() {
    return collector;
  }

  /**
   * Returns the {@link Lane} associated with {@code laneUri}, if it exists.
   *
   * @param laneUri to get the associated lane for.
   * @return the {@link Lane}
   * @throws IllegalArgumentException if no associated {@link Lane} is found.
   */
  public Lane getLane(String laneUri) {
    Integer id = laneMappings.get(laneUri);

    if (id == null) {
      throw new IllegalArgumentException("Unknown lane: " + laneUri);
    } else {
      return lanes.get(id).getLaneView();
    }
  }

  public TaskRegistry getTaskRegistry() {
    return taskRegistry;
  }

  public boolean isRunning() {
    return state == AgentState.Running;
  }

  public void setState(AgentState state) {
    this.state = state;
  }
}
