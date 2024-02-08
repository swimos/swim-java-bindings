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

package ai.swim.server.lanes.models.request;

import java.util.Objects;

/**
 * A {@link LaneRequest} which is tagged with its lane id.
 *
 * @param <T> the request's event type.
 */
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
