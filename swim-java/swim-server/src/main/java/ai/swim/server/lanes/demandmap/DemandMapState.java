package ai.swim.server.lanes.demandmap;

import ai.swim.codec.data.ByteWriter;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.PendingMapWrites;
import ai.swim.server.lanes.state.State;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.Iterator;
import java.util.UUID;

public class DemandMapState<K, V> implements State {
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final StateCollector collector;
  private final PendingMapWrites<K, V> pendingWrites;
  private final DemandMapLookup<K,V> lookup;
  private final int laneId;

  public DemandMapState(int laneId, DemandMapLaneView<K, V> view, StateCollector collector) {
    this.keyForm = view.keyForm();
    this.valueForm = view.valueForm();
    this.laneId = laneId;
    this.collector = collector;
    pendingWrites = new PendingMapWrites<>();
    lookup = new DemandMapLookup<>(view);
  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
    return pendingWrites.writeInto(laneId, lookup, bytes, keyForm, valueForm);
  }

  public void sync(UUID uuid, Iterator<K> keys) {
    pendingWrites.pushSync(uuid, keys);
    collector.add(this);
  }

  public void pushEvent(K key, V value) {
    pendingWrites.pushOperation(MapOperation.update(key, value));
    collector.add(this);
  }

}
