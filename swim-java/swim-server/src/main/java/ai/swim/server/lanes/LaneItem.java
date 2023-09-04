package ai.swim.server.lanes;

import ai.swim.codec.data.ByteWriter;

public interface LaneItem {
  WriteResult writeToBuffer(ByteWriter bytes);
}
