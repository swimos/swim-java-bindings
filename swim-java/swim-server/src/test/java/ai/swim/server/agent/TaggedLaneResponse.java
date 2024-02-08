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
