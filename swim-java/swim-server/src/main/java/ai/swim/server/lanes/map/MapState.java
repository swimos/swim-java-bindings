package ai.swim.server.lanes.map;

import ai.swim.codec.data.ByteWriter;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.state.State;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MapState<K, V> implements State {
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final StateCollector collector;
  private final List<MapEvent<K, V>> events;
  private final List<UUID> syncRequests;
  private final int laneId;
  private Map<K, V> state;
  private boolean dirty;

  public MapState(int laneId, Form<K> keyForm, Form<V> valueForm, StateCollector collector) {
    this.laneId = laneId;
    this.keyForm = keyForm;
    this.valueForm = valueForm;
    this.collector = collector;
    events = new ArrayList<>();
    syncRequests = new ArrayList<>();
  }

  /**
   * Clears the map.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public void clear() {
    CallContext.check();

    dirty = true;
    state.clear();
    events.add(MapEvent.clear());
    collector.add(this);
  }

  /**
   * Associates the specified value with the specified key in this map.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public void update(K key, V value) {
    CallContext.check();

    dirty = true;
    state.put(key, value);
    events.add(MapEvent.update(key, value));
    collector.add(this);
  }

  /**
   * Removes the mapping for a key from this map if it is present.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public void remove(K key) {
    CallContext.check();

    dirty = true;
    state.remove(key);
    events.add(MapEvent.remove(key));
    collector.add(this);
  }

  /**
   * Gets the value associated with the key.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public V get(K key) {
    CallContext.check();
    return state.get(key);
  }

//  private void write(Bytes buffer, LaneResponse<T> item) {
//    IdentifiedLaneResponseEncoder<T> encoder = new IdentifiedLaneResponseEncoder<>(new WithLenReconEncoder<>(form));
//    encoder.encode(new IdentifiedLaneResponse<>(laneId, item), buffer);
//  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
//    ListIterator<UUID> syncIter = syncRequests.listIterator();
//
//    while (syncIter.hasNext()) {
//      UUID remote = syncIter.next();
//
//      try {
//        write(bytes, LaneResponse.syncEvent(remote, state));
//        write(bytes, LaneResponse.synced(remote));
//      } catch (BufferOverflowException ignored) {
//        return WriteResult.DataStillAvailable;
//      }
//
//      syncIter.remove();
//    }
//
//    if (dirty) {
//      ListIterator<T> iter = events.listIterator();
//
//      while (iter.hasNext()) {
//        T item = iter.next();
//
//        try {
//          write(bytes, LaneResponse.event(item));
//        } catch (BufferOverflowException ignored) {
//          return WriteResult.DataStillAvailable;
//        }
//
//        iter.remove();
//      }
//
//      dirty = false;
//      return WriteResult.Done;
//    } else {
//      return WriteResult.NoData;
//    }

    return null;
  }

  @Override
  public void sync(UUID uuid) {
    syncRequests.add(uuid);
    collector.add(this);
  }

}
