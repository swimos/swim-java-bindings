package ai.swim.server.lanes.map;


public interface MapLookup<K,V> {
  V get(K key);
}
