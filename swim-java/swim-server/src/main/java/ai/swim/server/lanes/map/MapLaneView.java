package ai.swim.server.lanes.map;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.lifecycle.OnClear;
import ai.swim.server.lanes.lifecycle.OnRemove;
import ai.swim.server.lanes.lifecycle.OnUpdate;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;

public final class MapLaneView<K, V> extends LaneView implements MapLane<K, V> {
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private OnUpdate<K, V> onUpdate;
  private OnRemove<K, V> onRemove;
  private OnClear onClear;
  private MapLaneModel<K, V> model;

  public MapLaneView(Form<K> keyForm, Form<V> valueForm) {
    this.keyForm = keyForm;
    this.valueForm = valueForm;
  }

  @SuppressWarnings("unchecked")
  public void setModel(MapLaneModel<?, ?> model) {
    this.model = (MapLaneModel<K, V>) model;
  }

  @Override
  public LaneModel initLaneModel(StateCollector collector, int laneId) {
    MapLaneModel<K, V> model = new MapLaneModel<>(laneId, this, collector);
    this.model = model;
    return model;
  }

  @Override
  public Form<K> keyForm() {
    return keyForm;
  }

  @Override
  public Form<V> valueForm() {
    return valueForm;
  }

  @Override
  public MapLaneView<K, V> onUpdate(OnUpdate<K, V> onUpdate) {
    this.onUpdate = onUpdate;
    return this;
  }

  @Override
  public MapLaneView<K, V> onRemove(OnRemove<K, V> onRemove) {
    this.onRemove = onRemove;
    return this;
  }

  @Override
  public MapLaneView<K, V> onClear(OnClear onClear) {
    this.onClear = onClear;
    return this;
  }

  @Override
  public void onUpdate(K key, V oldValue, V newValue) {
    if (onUpdate != null) {
      onUpdate.onUpdate(key, oldValue, newValue);
    }
  }

  @Override
  public void onRemove(K key, V value) {
    if (onRemove != null) {
      onRemove.onRemove(key, value);
    }
  }

  @Override
  public void onClear() {
    if (onClear != null) {
      onClear.onClear();
    }
  }

  @Override
  public void clear() {
    model.clear();
    onClear();
  }

  @Override
  public V update(K key, V value) {
    V oldValue = model.update(key, value);
    onUpdate(key,oldValue,value);
    return oldValue;
  }

  @Override
  public V remove(K key) {
    V oldValue = model.remove(key);
    onRemove(key,oldValue);
    return oldValue;
  }

  @Override
  public V get(K key) {
    return model.get(key);
  }
}
