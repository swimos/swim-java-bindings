package ai.swim.server.agent;

import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import java.util.Objects;

public class TaggedLaneResponse<T> {
  private final String laneUri;
  private final boolean mapLike;
  private final LaneResponse<T> response;

  public TaggedLaneResponse(String laneUri, boolean mapLike,LaneResponse<T> response) {
    this.laneUri = laneUri;
    this.mapLike=mapLike;
    this.response = response;
  }

  public static <T> TaggedLaneResponse<T> value(String laneUri, LaneResponse<T> response){
    return new TaggedLaneResponse<>(laneUri,false,response);
  }

  public static <T> TaggedLaneResponse<T> map(String laneUri,LaneResponse<T> response){
    return new TaggedLaneResponse<>(laneUri,true,response);
  }

  public String getLaneUri() {
    return laneUri;
  }

  public LaneResponse<T> getResponse() {
    return response;
  }

  public boolean isMapLike() {
    return mapLike;
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
