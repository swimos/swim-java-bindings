package ai.swim.server.lanes.demand;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.state.StateCollector;
import java.util.UUID;

public final class DemandLaneModel<T> extends LaneModel {
  private final DemandLaneView<T> view;
  private final DemandState<T> state;

  public DemandLaneModel(int laneId, DemandLaneView<T> view, StateCollector collector) {
    this.view = view;
    this.state = new DemandState<>(laneId,view.valueForm(), collector);
  }

  @Override
  public void dispatch(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public void sync(UUID remote) {
    T value = view.onCue();
    state.sync(remote, value);
  }

  @Override
  public void init(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  public void cue(T value) {
    state.cue(value);
  }

}
