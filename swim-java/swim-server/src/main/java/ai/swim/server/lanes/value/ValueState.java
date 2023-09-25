package ai.swim.server.lanes.value;

import ai.swim.server.codec.BufferOverflowException;
import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.WithLenReconEncoder;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponseEncoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.state.State;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class ValueState<T> implements State {
  private final Form<T> form;
  private final StateCollector collector;
  private final List<T> events;
  private final List<UUID> syncRequests;
  private final int laneId;
  private T state;
  private boolean dirty;

  public ValueState(int laneId, Form<T> form, StateCollector collector) {
    this.laneId = laneId;
    this.form = form;
    this.collector = collector;
    events = new ArrayList<>();
    syncRequests = new ArrayList<>();
  }

  public T set(T to) {
    T oldState = state;
    dirty = true;
    state = to;
    events.add(to);
    collector.add(this);
    return oldState;
  }

  public T get() {
    return state;
  }

  private void write(Bytes buffer, LaneResponse<T> item) {
    IdentifiedLaneResponseEncoder<T> encoder = new IdentifiedLaneResponseEncoder<>(new WithLenReconEncoder<>(form));
    encoder.encode(new IdentifiedLaneResponse<>(laneId, item), buffer);
  }

  @Override
  public WriteResult writeInto(Bytes bytes) {
    ListIterator<UUID> syncIter = syncRequests.listIterator();

    while (syncIter.hasNext()) {
      UUID remote = syncIter.next();

      try {
        write(bytes, LaneResponse.syncEvent(remote, state));
        write(bytes, LaneResponse.synced(remote));
      } catch (BufferOverflowException ignored) {
        return WriteResult.DataStillAvailable;
      }

      syncIter.remove();
    }

    if (dirty) {
      ListIterator<T> iter = events.listIterator();

      while (iter.hasNext()) {
        T item = iter.next();

        try {
          write(bytes, LaneResponse.event(item));
        } catch (BufferOverflowException ignored) {
          return WriteResult.DataStillAvailable;
        }

        iter.remove();
      }

      dirty = false;
      return WriteResult.Done;
    } else {
      return WriteResult.NoData;
    }
  }

  @Override
  public void sync(UUID uuid) {
    syncRequests.add(uuid);
    collector.add(this);
  }

}
