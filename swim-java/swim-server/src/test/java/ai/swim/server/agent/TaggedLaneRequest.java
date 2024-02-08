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
import java.util.Objects;

public class TaggedLaneRequest<T> {
  private final String laneUri;
  private final boolean mapLike;
  private final LaneRequest<T> request;

  private TaggedLaneRequest(String laneUri, boolean mapLike,LaneRequest<T> request) {
    this.laneUri = laneUri;
    this.mapLike=mapLike;
    this.request = request;
  }

  public static <T> TaggedLaneRequest<T> value(String laneUri,LaneRequest<T> request){
    return new TaggedLaneRequest<>(laneUri,false,request);
  }

  public static <T> TaggedLaneRequest<T> map(String laneUri,LaneRequest<T> request){
    return new TaggedLaneRequest<>(laneUri,true,request);
  }

  public String getLaneUri() {
    return laneUri;
  }

  public LaneRequest<T> getRequest() {
    return request;
  }

  public boolean isMapLike() {
    return mapLike;
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
