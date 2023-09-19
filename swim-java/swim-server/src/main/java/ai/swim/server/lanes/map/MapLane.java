package ai.swim.server.lanes.map;

import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.lifecycle.OnClear;
import ai.swim.server.lanes.lifecycle.OnRemove;
import ai.swim.server.lanes.lifecycle.OnUpdate;
import ai.swim.structure.Form;

public interface MapLane<K, V> extends Lane {
  Form<K> keyForm();

  Form<V> valueForm();

  MapLaneView<K, V> onUpdate(OnUpdate<K, V> onUpdate);

  MapLaneView<K, V> onRemove(OnRemove<K, V> onRemove);

  MapLaneView<K, V> onClear(OnClear onClear);

  void onUpdate(K key, V oldValue, V newValue);

  void onRemove(K key, V value);

  void onClear();

  void clear();

  V update(K key, V value);

  V remove(K key);

  V get(K key);
}
