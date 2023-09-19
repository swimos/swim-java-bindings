package ai.swim.server.lanes.state;

import ai.swim.codec.data.BufferOverflowException;
import ai.swim.codec.data.ByteWriter;
import ai.swim.server.agent.AgentView;
import ai.swim.server.lanes.WriteResult;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Manages the state of all the lanes on an {@link AgentView}. Lanes may register that their state
 * is now dirty and requires flushing to their peers.
 * <p>
 * When the state of this collector is flushed, a byte buffer is returned that has the following layout:
 * <ul>
 *   <li>[0]: boolean representing whether there is more data available</li>
 *   <li>[1..]: elements are the responses from lanes. These are {@link ai.swim.server.lanes.models.response.IdentifiedLaneResponse}s. Where the
 *   first 4 elements are an integer containing the lane identifier and the remaining element's are a lane response.
 *   </li>
 * </ul>
 */
public class StateCollector {
  private final Set<State> stack;
  private ByteWriter buffer;

  public StateCollector() {
    this.buffer = new ByteWriter();
    this.stack = new HashSet<>();
  }

  /**
   * Notifies this {@link StateCollector} that {@code state} requires flushing.
   *
   * @param state to register.
   */
  public void add(State state) {
    stack.add(state);
  }

  /**
   * Flushes the state of all the lanes that have been registered.
   * <p>
   * See this class's root-level documentation for the array's layout.
   *
   * @return a buffer containing the state of all the lanes that were registered.
   */
  public byte[] flushState() {
    WriteResult writeResult = WriteResult.NoData;
    Iterator<State> iter = stack.iterator();

    int startIdx = buffer.writePosition();
    buffer.writeByte((byte) 0);

    while (iter.hasNext()) {
      State dirty = iter.next();
      try {
        if (dirty.writeInto(buffer).done()) {
          iter.remove();
        } else {
          writeResult = WriteResult.DataStillAvailable;
        }
      } catch (BufferOverflowException ignored) {
        writeResult = WriteResult.DataStillAvailable;
        break;
      }
    }

    buffer.writeByte(writeResult.statusCode(), startIdx);

    byte[] data = buffer.getArray();
    buffer = new ByteWriter();

    return data;
  }
}
