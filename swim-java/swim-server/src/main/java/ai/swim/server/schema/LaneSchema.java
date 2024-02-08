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

package ai.swim.server.schema;

import ai.swim.server.lanes.command.CommandLane;
import ai.swim.server.lanes.demand.DemandLane;
import ai.swim.server.lanes.demandmap.DemandMapLane;
import ai.swim.server.lanes.map.MapLane;
import ai.swim.server.lanes.supply.SupplyLane;
import ai.swim.server.lanes.value.ValueLane;
import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;
import java.util.Objects;

public class LaneSchema {
  private final boolean isTransient;
  private final LaneKind laneKind;
  private final int laneId;

  public LaneSchema(boolean isTransient, LaneKind laneKind, int laneId) {
    this.isTransient = isTransient;
    this.laneKind = laneKind;
    this.laneId = laneId;
  }

  public static LaneSchema reflectLane(Class<?> type, boolean isTransient, int laneId) {
    if (ValueLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.Value, laneId);
    } else if (MapLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.Map, laneId);
    } else if (DemandLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.Demand, laneId);
    } else if (DemandMapLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.DemandMap, laneId);
    } else if (SupplyLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.Supply, laneId);
    } else if (CommandLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.Command, laneId);
    } else {
      throw new IllegalArgumentException("Unsupported lane type: " + type);
    }
  }

  public boolean isTransient() {
    return isTransient;
  }

  public LaneKind getLaneKind() {
    return laneKind;
  }

  @Override
  public String toString() {
    return "LaneSchema{" + "isTransient=" + isTransient + ", laneKind=" + laneKind + ", laneId=" + laneId + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LaneSchema laneSchema = (LaneSchema) o;
    return isTransient == laneSchema.isTransient && laneKind == laneSchema.laneKind && laneId == laneSchema.laneId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(isTransient, laneKind, laneId);
  }

  public void pack(MessageBufferPacker packer) throws IOException {
    packer.packArrayHeader(3);
    packer.packBoolean(isTransient);
    packer.packInt(laneId);
    laneKind.pack(packer);
  }

  public int getLaneId() {
    return laneId;
  }
}
