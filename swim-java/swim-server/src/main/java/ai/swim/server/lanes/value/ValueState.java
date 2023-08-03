package ai.swim.server.lanes.value;

import ai.swim.server.codec.BufferOverflowException;
import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.WithLenReconEncoder;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.models.response.LaneResponseEncoder;
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
  private T state;
  private boolean dirty;
  private final List<T> events;

  public ValueState(Form<T> form, StateCollector collector) {
    this.form = form;
    this.collector = collector;
    events = new ArrayList<>();
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
    LaneResponseEncoder<T> responseEncoder = new LaneResponseEncoder<>(new WithLenReconEncoder<>(form));
    responseEncoder.encode(item, buffer);
  }

  @Override
  public WriteResult writeInto(Bytes bytes) {
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
  public byte[] syncInto(UUID uuid) {
    Bytes bytes = new Bytes();

    try {
      bytes.writeByte((byte) 0);
      if (state != null) {
        write(bytes, LaneResponse.syncEvent(uuid, state));
      }
      write(bytes, LaneResponse.synced(uuid));
      bytes.writeByte(WriteResult.Done.statusCode(), 0);
    } catch (BufferOverflowException ignored) {
      bytes.writeByte(WriteResult.DataStillAvailable.statusCode(), 0);
    }

    return bytes.getArray();
  }

}
