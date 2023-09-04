package ai.swim.server.lanes.map;

import ai.swim.codec.data.ByteWriter;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.nio.ByteBuffer;
import java.util.UUID;

public final class MapLaneModel<K, V> extends LaneModel {
  private final MapLaneView<K, V> view;
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final MapState<K, V> state;

  public MapLaneModel(int laneId, MapLaneView<K, V> view, StateCollector collector) {
    this.view = view;
    this.keyForm = view.keyForm();
    this.valueForm = view.valueForm();
    this.state = new MapState<>(laneId, keyForm, valueForm, collector);
  }

  @Override
  public void dispatch(ByteBuffer buffer) {

  }

  @Override
  public void sync(UUID remote) {
    state.sync(remote);
  }

  @Override
  public void init(ByteBuffer buffer) {

  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  @Override
  public WriteResult writeToBuffer(ByteWriter bytes) {
    return state.writeInto(bytes);
  }

  public void clear() {
    state.clear();
  }

  public void update(K key, V value) {
    state.update(key, value);
  }

  public void remove(K key) {
    state.remove(key);
  }

  public V get(K key) {
    return state.get(key);
  }

}
