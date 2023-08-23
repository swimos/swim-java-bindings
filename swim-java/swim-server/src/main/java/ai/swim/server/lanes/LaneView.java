package ai.swim.server.lanes;

import ai.swim.server.lanes.state.StateCollector;

public abstract class LaneView implements Lane {

  public abstract LaneModel initLaneModel(StateCollector collector, int laneId);

}
