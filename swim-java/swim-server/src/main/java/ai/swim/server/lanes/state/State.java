package ai.swim.server.lanes.state;

import ai.swim.server.codec.Bytes;
import ai.swim.server.lanes.WriteResult;
import java.util.UUID;

public interface State {
  WriteResult writeInto(Bytes bytes);

  void sync(UUID uuid);

}
