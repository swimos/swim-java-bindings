package ai.swim.server.lanes.state;

import ai.swim.server.codec.BufferOverflowException;
import ai.swim.server.codec.Bytes;
import ai.swim.server.lanes.WriteResult;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StateCollector {
  private final Set<State> stack;
  private Bytes buffer;

  public StateCollector() {
    this.buffer = new Bytes();
    this.stack = new HashSet<>();
  }

  public void add(State state) {
    stack.add(state);
  }

  public byte[] flushState() {
    WriteResult writeResult = WriteResult.NoData;
    Iterator<State> iter = stack.iterator();

    int startIdx = buffer.remaining();
    buffer.writeInteger(0);
    int startLen = buffer.remaining();

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

    buffer.writeInteger(buffer.remaining() - startLen, startIdx);
    buffer.writeByte(writeResult.statusCode());

    byte[] data = buffer.getArray();
    buffer = new Bytes();

    return data;
  }
}
