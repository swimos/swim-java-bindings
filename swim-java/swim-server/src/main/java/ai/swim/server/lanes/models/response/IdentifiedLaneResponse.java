package ai.swim.server.lanes.models.response;

import java.util.Objects;

/**
 * A {@link LaneResponse} which is tagged with its lane id.
 * @param <T> the responses event type.
 */
public class IdentifiedLaneResponse<T> {
  private final int laneId;
  private final LaneResponse<T> laneResponse;

  public IdentifiedLaneResponse(int laneId, LaneResponse<T> laneResponse) {
    this.laneId = laneId;
    this.laneResponse = laneResponse;
  }

  public LaneResponse<T> getLaneResponse() {
    return laneResponse;
  }

  public int getLaneId() {
    return laneId;
  }

  @Override
  public String toString() {
    return "IdentifiedLaneResponse{" +
        "laneId=" + laneId +
        ", laneResponse=" + laneResponse +
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
    IdentifiedLaneResponse<?> that = (IdentifiedLaneResponse<?>) o;
    return laneId == that.laneId && Objects.equals(laneResponse, that.laneResponse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(laneId, laneResponse);
  }
}
