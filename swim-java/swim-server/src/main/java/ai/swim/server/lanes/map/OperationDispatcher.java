package ai.swim.server.lanes.map;

public class OperationDispatcher<V, K> implements MapOperationVisitor<K, V> {
  private final MapState<K, V> state;
  private final MapLaneView<K, V> view;

  public OperationDispatcher(MapState<K, V> state, MapLaneView<K, V> view) {
    this.state = state;
    this.view = view;
  }

  @Override
  public void visitUpdate(K key, V value) {
    V oldValue = state.update(key, value);
    view.onUpdate(key, oldValue, value);
  }

  @Override
  public void visitRemove(K key) {
    V oldValue = state.remove(key);
    view.onRemove(key, oldValue);
  }

  @Override
  public void visitClear() {
    state.clear();
    view.onClear();
  }
}
