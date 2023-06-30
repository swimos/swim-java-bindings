package ai.swim.server.agent.schema;

import java.lang.reflect.Field;
import java.util.Objects;

public class LaneSpec {
  private final boolean isTransient;
  private final LaneKind laneKind;

  public LaneSpec(boolean isTransient, LaneKind laneKind) {
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
    return "LaneSpec{" +
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
    LaneSpec laneSpec = (LaneSpec) o;
    return isTransient == laneSpec.isTransient && laneKind == laneSpec.laneKind;
  }

  @Override
  public int hashCode() {
    return Objects.hash(isTransient, laneKind);
  }
}
