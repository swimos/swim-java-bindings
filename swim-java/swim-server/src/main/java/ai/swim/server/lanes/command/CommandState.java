package ai.swim.server.lanes.command;

import ai.swim.codec.data.BufferOverflowException;
import ai.swim.codec.data.ByteWriter;
import ai.swim.server.agent.call.CallContext;
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

public class CommandState<T> implements State {
  private final Form<T> form;
  private final StateCollector collector;
  private final List<T> events;
  private final List<UUID> syncRequests;
  private final int laneId;
  private boolean dirty;

  public CommandState(int laneId, Form<T> form, StateCollector collector) {
    this.laneId = laneId;
    this.form = form;
    this.collector = collector;
    events = new ArrayList<>();
    syncRequests = new ArrayList<>();
  }

  /**
   * Sets the current state.
   *
   * @throws ai.swim.server.agent.call.CallContextException if not invoked from a valid call context.
   */
  public void command(T value) {
    CallContext.check();

    dirty = true;
    events.add(value);
    collector.add(this);
  }

  private void write(ByteWriter buffer, LaneResponse<T> item) {
    IdentifiedLaneResponseEncoder<T> encoder = new IdentifiedLaneResponseEncoder<>(new WithLenReconEncoder<>(form));
    encoder.encode(new IdentifiedLaneResponse<>(laneId, item), buffer);
  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
    ListIterator<UUID> syncIter = syncRequests.listIterator();

    while (syncIter.hasNext()) {
      UUID remote = syncIter.next();

      try {
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
