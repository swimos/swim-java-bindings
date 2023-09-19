package ai.swim.server.agent;

import ai.swim.server.lanes.models.response.LaneResponse;
import java.util.Objects;

public class TaggedLaneResponse<T> {
  private final String laneUri;
  private final LaneResponse<T> response;

  public TaggedLaneResponse(String laneUri, LaneResponse<T> response) {
    this.laneUri = laneUri;
    this.response = response;
  }

  public String getLaneUri() {
    return laneUri;
  }

  public LaneResponse<T> getResponse() {
    return response;
  }

  @Override
  public String toString() {
    return "TaggedLaneResponse{" +
        "laneUri='" + laneUri + '\'' +
        ", response=" + response +
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
    TaggedLaneResponse<?> that = (TaggedLaneResponse<?>) o;
    return Objects.equals(laneUri, that.laneUri) && Objects.equals(response, that.response);
  }

  @Override
  public int hashCode() {
    return Objects.hash(laneUri, response);
  }
}
