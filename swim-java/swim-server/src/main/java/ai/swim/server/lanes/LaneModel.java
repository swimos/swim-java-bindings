package ai.swim.server.lanes;

import ai.swim.structure.recognizer.RecognizerException;
import java.nio.ByteBuffer;
import java.util.UUID;

public abstract class LaneModel implements LaneItem {
  public abstract void dispatch(ByteBuffer buffer) throws RecognizerException;

  public abstract void sync(UUID remote);

  public abstract void init(ByteBuffer buffer);

  public abstract LaneView getLaneView();
}
