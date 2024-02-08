package ai.swim.server.lanes.demand;

import ai.swim.codec.data.BufferOverflowException;
import ai.swim.codec.data.ByteWriter;
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

public class DemandState<T> implements State {
  private final Form<T> form;
  private final StateCollector collector;
  private final List<T> events;
  private final List<LaneResponse<T>> syncRequests;
  private final int laneId;

  public DemandState(int laneId, Form<T> form, StateCollector collector) {
    this.form = form;
    this.laneId = laneId;
    this.collector = collector;
    events = new ArrayList<>();
    syncRequests = new ArrayList<>();
  }

  private void write(ByteWriter buffer, LaneResponse<T> item) {
    IdentifiedLaneResponseEncoder<T> encoder = new IdentifiedLaneResponseEncoder<>(new WithLenReconEncoder<>(form));
    encoder.encode(new IdentifiedLaneResponse<>(laneId, item), buffer);
  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
    ListIterator<LaneResponse<T>> syncIter = syncRequests.listIterator();

    boolean wroteSync = syncIter.hasNext();

    while (syncIter.hasNext()) {
      LaneResponse<T> response = syncIter.next();

      try {
        write(bytes, response);
      } catch (BufferOverflowException ignored) {
        return WriteResult.DataStillAvailable;
      }

      syncIter.remove();
    }

    ListIterator<T> iter = events.listIterator();

    if (iter.hasNext()) {
      while (iter.hasNext()) {
        T item = iter.next();

        try {
          write(bytes, LaneResponse.event(item));
        } catch (BufferOverflowException ignored) {
          return WriteResult.DataStillAvailable;
        }

        iter.remove();
      }
      return WriteResult.Done;
    } else {
      return wroteSync ? WriteResult.Done : WriteResult.NoData;
    }
  }

  public void sync(UUID uuid, T value) {
    if (value != null) {
      syncRequests.add(LaneResponse.syncEvent(uuid, value));
    }
    syncRequests.add(LaneResponse.synced(uuid));
    collector.add(this);
  }

  public void cue(T value) {
    if (value != null) {
      events.add(value);
      collector.add(this);
    }
  }

}
