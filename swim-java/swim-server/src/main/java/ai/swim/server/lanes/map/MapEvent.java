package ai.swim.server.lanes.map;

class MapEvent<K, V> {
  static <K, V> MapEvent<K, V> update(K key, V value) {
    return new UpdateEvent<>(key, value);
  }

  static <K, V> MapEvent<K, V> remove(K key) {
    return new RemoveEvent<>(key);
  }

  static <K, V> MapEvent<K, V> clear() {
    return new ClearEvent<>();
  }

  private static class UpdateEvent<K, V> extends MapEvent<K, V> {
    private final K key;
    private final V value;

    public UpdateEvent(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }

  private static class RemoveEvent<K, V> extends MapEvent<K, V> {
    private final K key;

    public RemoveEvent(K key) {
      this.key = key;
    }
  }

  private static class ClearEvent<K, V> extends MapEvent<K, V> {
  }
}
