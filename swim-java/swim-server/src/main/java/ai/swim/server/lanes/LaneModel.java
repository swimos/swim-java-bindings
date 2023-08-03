package ai.swim.server.lanes;

import ai.swim.server.codec.Bytes;
import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class LaneModel implements LaneItem {
  public abstract void dispatch(ByteBuffer buffer);

  public abstract byte[] sync(UUID remote);

  public abstract void init(ByteBuffer buffer);
}
