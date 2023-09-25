package ai.swim.server.lanes.demandmap;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.lifecycle.OnCueKey;
import ai.swim.server.lanes.lifecycle.OnSyncKeys;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.Collections;
import java.util.Iterator;

public final class DemandMapLaneView<K, V> extends LaneView implements DemandMapLane<K, V> {
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private OnCueKey<K, V> onCueKey;
  private OnSyncKeys<K> onSyncKeys;
  private DemandMapLaneModel<K, V> model;

  public DemandMapLaneView(Form<K> keyForm, Form<V> valueForm) {
    this.keyForm = keyForm;
    this.valueForm = valueForm;
  }

  @SuppressWarnings({"unchecked", "unused"})
  public void setModel(DemandMapLaneModel<?, ?> model) {
    this.model = (DemandMapLaneModel<K, V>) model;
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
  public DemandMapLaneView<K, V> onCueKey(OnCueKey<K, V> onCueKey) {
    this.onCueKey = onCueKey;
    return this;
  }

  @Override
  public DemandMapLaneView<K, V> onSyncKeys(OnSyncKeys<K> onSyncKeys) {
    this.onSyncKeys = onSyncKeys;
    return this;
  }

  @Override
  public V onCueKey(K key) {
    if (onCueKey != null) {
      return onCueKey.onCueKey(key);
    } else {
      return null;
    }
  }

  @Override
  public Iterator<K> onSyncKeys() {
    if (onSyncKeys != null) {
      return onSyncKeys.onSyncKeys();
    } else {
      return Collections.emptyIterator();
    }
  }

  @Override
  public void cueKey(K key) {
    V value = onCueKey(key);
    if (value != null) {
      model.pushEvent(key, value);
    }
  }

  @Override
  public LaneModel initLaneModel(StateCollector collector, int laneId) {
    DemandMapLaneModel<K, V> model = new DemandMapLaneModel<>(laneId, this, collector);
    this.model = model;
    return model;
  }

}
