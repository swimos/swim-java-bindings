package ai.swim.server.lanes;

public interface MapLookup<K,V> {
  V get(K key);
}
