package ai.swim.server.agent;

import ai.swim.server.lanes.models.request.LaneRequest;
import java.util.Objects;

public class TaggedLaneRequest<T> {
  private final String laneUri;
  private final LaneRequest<T> request;

  public TaggedLaneRequest(String laneUri, LaneRequest<T> request) {
    this.laneUri = laneUri;
    this.request = request;
  }

  public String getLaneUri() {
    return laneUri;
  }

  public LaneRequest<T> getRequest() {
    return request;
  }

  @Override
  public String toString() {
    return "TaggedLaneRequest{" +
        "laneUri='" + laneUri + '\'' +
        ", request=" + request +
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
    TaggedLaneRequest<?> that = (TaggedLaneRequest<?>) o;
    return Objects.equals(laneUri, that.laneUri) && Objects.equals(request, that.request);
  }

  @Override
  public int hashCode() {
    return Objects.hash(laneUri, request);
  }
}
