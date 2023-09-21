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
