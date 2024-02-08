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

package ai.swim.server.lanes;

import ai.swim.server.lanes.state.StateCollector;

public abstract class LaneView implements Lane {

  /**
   * Initialises the lane model associated with this {@link LaneView}.
   *
   * @param collector the agent's {@link StateCollector}.
   * @param laneId    the ID of this lane.
   * @return an initialised {@link LaneModel}.
   */
  public abstract LaneModel initLaneModel(StateCollector collector, int laneId);

}
