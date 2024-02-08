package ai.swim.server.lanes.supply;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.state.StateCollector;
import java.util.UUID;

public final class SupplyLaneModel<T> extends LaneModel {
  private final SupplyLaneView<T> view;
  private final SupplyState<T> state;

  public SupplyLaneModel(int laneId, SupplyLaneView<T> view, StateCollector collector) {
    this.view = view;
    this.state = new SupplyState<>(laneId, view.valueForm(), collector);
  }

  @Override
  public void dispatch(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public void sync(UUID remote) {
    state.sync(remote);
  }

  @Override
  public void init(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  public void push(T value) {
    state.push(value);
  }

}
