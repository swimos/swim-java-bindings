package ai.swim.server.lanes.map;

public class Initialiser<V, K> implements MapOperationVisitor<K, V> {
  private final MapState<K, V> state;

  public Initialiser(MapState<K, V> state) {
    this.state = state;
  }

  @Override
  public void visitUpdate(K key, V value) {
    state.update(key, value);
  }

  @Override
  public void visitRemove(K key) {
    state.remove(key);
  }

  @Override
  public void visitClear() {
    state.clear();
  }
}
