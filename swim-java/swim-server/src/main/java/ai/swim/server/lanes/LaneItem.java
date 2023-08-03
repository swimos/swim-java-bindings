package ai.swim.server.lanes;

import ai.swim.server.codec.Bytes;

public interface LaneItem {
  WriteResult writeToBuffer(Bytes bytes);
}
