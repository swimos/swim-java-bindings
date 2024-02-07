package ai.swim.server.lanes.map;

public interface MapOperationVisitor<K, V> {
  void visitUpdate(K key, V value);

  void visitRemove(K key);

  void visitClear();
}
