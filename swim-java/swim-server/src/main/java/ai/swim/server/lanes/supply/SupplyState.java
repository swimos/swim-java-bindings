package ai.swim.server.lanes.supply;

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

public class SupplyState<T> implements State {
  private final Form<T> form;
  private final StateCollector collector;
  private final List<LaneResponse<T>> events;
  private final int laneId;

  public SupplyState(int laneId, Form<T> form, StateCollector collector) {
    this.form = form;
    this.laneId = laneId;
    this.collector = collector;
    events = new ArrayList<>();
  }

  private void write(ByteWriter buffer, LaneResponse<T> item) {
    IdentifiedLaneResponseEncoder<T> encoder = new IdentifiedLaneResponseEncoder<>(new WithLenReconEncoder<>(form));
    encoder.encode(new IdentifiedLaneResponse<>(laneId, item), buffer);
  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
    ListIterator<LaneResponse<T>> iter = events.listIterator();

    if (iter.hasNext()) {
      while (iter.hasNext()) {
        LaneResponse<T> item = iter.next();

        try {
          write(bytes, item);
        } catch (BufferOverflowException ignored) {
          return WriteResult.DataStillAvailable;
        }

        iter.remove();
      }
      return WriteResult.Done;
    } else {
      return WriteResult.NoData;
    }
  }

  public void sync(UUID uuid) {
    events.add(LaneResponse.synced(uuid));
    collector.add(this);
  }

  public void push(T value) {
    if (value != null) {
      events.add(LaneResponse.event(value));
      collector.add(this);
    }
  }

}
