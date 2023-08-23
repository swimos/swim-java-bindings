package ai.swim.server.lanes.models.request;

import java.util.Objects;

public class IdentifiedLaneRequest<T> {
  private final int laneId;
  private final LaneRequest<T> laneRequest;

  public IdentifiedLaneRequest(int laneId, LaneRequest<T> laneRequest) {
    this.laneId = laneId;
    this.laneRequest = laneRequest;
  }

  public LaneRequest<T> getLaneRequest() {
    return laneRequest;
  }

  public int getLaneId() {
    return laneId;
  }

  @Override
  public String toString() {
    return "IdentifiedLaneRequest{" +
        "laneId=" + laneId +
        ", LaneRequest=" + laneRequest +
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
    IdentifiedLaneRequest<?> that = (IdentifiedLaneRequest<?>) o;
    return laneId == that.laneId && Objects.equals(laneRequest, that.laneRequest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(laneId, laneRequest);
  }
}
