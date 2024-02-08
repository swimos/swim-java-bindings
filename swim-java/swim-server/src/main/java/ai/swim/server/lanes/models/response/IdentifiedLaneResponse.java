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
