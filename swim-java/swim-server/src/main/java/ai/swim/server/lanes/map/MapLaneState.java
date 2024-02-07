package ai.swim.server.lanes.map;

<<<<<<<< HEAD:swim-java/swim-server/src/main/java/ai/swim/server/lanes/map/MapLaneState.java
import ai.swim.codec.data.ByteWriter;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.agent.call.CallContextException;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.state.State;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MapLaneState<K, V> implements State {
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final StateCollector collector;
  private final PendingWrites<K, V> pendingWrites;
  private final int laneId;
  private final TypedHashMap<K, V> state;

  public MapLaneState(int laneId, Form<K> keyForm, Form<V> valueForm, StateCollector collector) {
    this.laneId = laneId;
    this.keyForm = keyForm;
    this.valueForm = valueForm;
    this.collector = collector;
    state = new TypedHashMap<>();
    pendingWrites = new PendingWrites<>();
========
import ai.swim.server.lanes.MapLookup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapState<K, V> implements MapLookup<K, V> {
  private final Map<K, V> state;

  public MapState() {
    state = new HashMap<>();
>>>>>>>> origin/supply-lane:swim-java/swim-server/src/main/java/ai/swim/server/lanes/map/MapState.java
  }

  public MapState(Map<K, V> state) {
    this.state = state;
  }

  @Override
  public V get(K key) {
    return state.get(key);
  }

  public void clear() {
    state.clear();
  }

<<<<<<<< HEAD:swim-java/swim-server/src/main/java/ai/swim/server/lanes/map/MapLaneState.java
  public int size() {
    return state.size();
  }

  public boolean containsKey(K key) {
    return state.containsKey(key);
  }

  public boolean containsValue(V value) {
    return state.containsValue(value);
  }

  public void putAll(TypedMap<? extends K, ? extends V> m) {
    state.putAll(m);
  }

  public Set<K> keySet() {
    return state.keySet();
  }

  public Collection<V> values() {
    return state.values();
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return state.entrySet();
  }
}
========
  public V put(K key, V value) {
    return state.put(key, value);
  }

  public V remove(K key) {
    return state.remove(key);
  }

  public Set<K> keySet() {
    return new HashSet<>(state.keySet());
  }
}
>>>>>>>> origin/supply-lane:swim-java/swim-server/src/main/java/ai/swim/server/lanes/map/MapState.java
