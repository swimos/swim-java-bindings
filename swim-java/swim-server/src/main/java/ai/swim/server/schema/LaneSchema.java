package ai.swim.server.schema;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.value.ValueLane;
import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;
import java.util.Objects;

/**
 * A schema representing Swim Lane that may be serialized into a msgpack representation for providing to the Rust
 * runtime.
 */
public class LaneSchema {
  /**
   * Whether the lane is transient.
   */
  private final boolean isTransient;
  /**
   * The type of the lane.
   */
  private final LaneKind laneKind;
  /**
   * A unique identifier for the lane.
   */
  private final int laneId;

  public LaneSchema(boolean isTransient, LaneKind laneKind, int laneId) {
    this.isTransient = isTransient;
    this.laneKind = laneKind;
    this.laneId = laneId;
  }

  /**
   * Reflects a lane from a {@link Class}.
   *
   * @param type        of the lane. This must extend {@link Lane}
   * @param isTransient whether the lane is transient
   * @param laneId      a unique identifier for the lane
   * @return a schema for the lane
   */
  public static LaneSchema reflectLane(Class<?> type, boolean isTransient, int laneId) {
    if (ValueLane.class.isAssignableFrom(type)) {
      return new LaneSchema(isTransient, LaneKind.Value, laneId);
    } else {
      throw new IllegalArgumentException("Unsupported lane type: " + type);
    }
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

  /**
   * Packs this {@link LaneSchema} into a msgpack representation
   *
   * @param packer to use for writing the representation
   * @throws IOException if there is an exception thrown when writing into the {@link MessageBufferPacker}
   */
  public void pack(MessageBufferPacker packer) throws IOException {
    packer.packArrayHeader(3);
    packer.packBoolean(isTransient);
    packer.packInt(laneId);
    laneKind.pack(packer);
  }

  /**
   * Returns the unique identifier for the lane
   *
   * @return the unique identifier for the lane
   */
  public int getLaneId() {
    return laneId;
  }
}
