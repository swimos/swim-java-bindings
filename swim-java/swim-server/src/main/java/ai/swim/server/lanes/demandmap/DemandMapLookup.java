package ai.swim.server.lanes.demandmap;

import ai.swim.server.lanes.MapLookup;

public class DemandMapLookup<K, V> implements MapLookup<K, V> {
  private final DemandMapLaneView<K, V> view;

  public DemandMapLookup(DemandMapLaneView<K, V> view) {
    this.view = view;
  }

  @Override
  public V get(K key) {
    return view.onCueKey(key);
  }
}
