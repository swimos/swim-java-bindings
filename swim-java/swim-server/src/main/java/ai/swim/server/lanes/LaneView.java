package ai.swim.server.lanes;

import ai.swim.server.lanes.state.StateCollector;

public abstract class LaneView implements Lane {

  public abstract LaneModel createLaneModel(StateCollector collector, int laneId);

}
