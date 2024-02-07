package ai.swim.server.schema;

import ai.swim.server.lanes.demand.DemandLane;
import ai.swim.server.lanes.map.MapLane;
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
