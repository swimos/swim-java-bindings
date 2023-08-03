package ai.swim.server.schema;

import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;
import java.util.Objects;

public class LaneSchema {
  private final boolean isTransient;
  private final LaneKind laneKind;

  public LaneSchema(boolean isTransient, LaneKind laneKind) {
    this.isTransient = isTransient;
    this.laneKind = laneKind;
  }

  public boolean isTransient() {
    return isTransient;
  }

  public LaneKind getLaneKind() {
    return laneKind;
  }

  @Override
  public String toString() {
    return "LaneSchema{" +
        "isTransient=" + isTransient +
        ", laneKind=" + laneKind +
        '}';
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
    return isTransient == laneSchema.isTransient && laneKind == laneSchema.laneKind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(isTransient, laneKind);
  }

  public void pack(MessageBufferPacker packer) throws IOException {
    packer.packArrayHeader(2);
    packer.packBoolean(isTransient);
    laneKind.pack(packer);
  }
}
