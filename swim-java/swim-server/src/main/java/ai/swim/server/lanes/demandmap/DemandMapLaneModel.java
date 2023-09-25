package ai.swim.server.lanes.demandmap;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.state.StateCollector;
import java.util.Iterator;
import java.util.UUID;

public final class DemandMapLaneModel<K, V> extends LaneModel {
  private final DemandMapLaneView<K, V> view;
  private final DemandMapState<K, V> state;

  public DemandMapLaneModel(int laneId, DemandMapLaneView<K, V> view, StateCollector collector) {
    this.view = view;
    this.state = new DemandMapState<>(laneId, view, collector);
  }

  @Override
  public void dispatch(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public void sync(UUID remote) {
    Iterator<K> iterator = view.onSyncKeys();
    state.sync(remote, iterator);
  }

  @Override
  public void init(ReadBuffer buffer) {
    // no-op
  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  public void pushEvent(K key, V value) {
    state.pushEvent(key, value);
  }

}
