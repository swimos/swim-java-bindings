package ai.swim.server.lanes.state;

import ai.swim.server.codec.Bytes;
import ai.swim.server.lanes.WriteResult;
import java.util.UUID;

/**
 * State definition interface for operating with a {@link StateCollector}.
 */
public interface State {
  /**
   * Write the current state of the lane into {@code bytes}.
   *
   * @param bytes to write into.
   * @return a result stating whether there is more data to write.
   */
  WriteResult writeInto(Bytes bytes);

  /**
   * Register {@code uuid}'s intent to sync with the lane.
   *
   * @param uuid remote to sync with.
   */
  void sync(UUID uuid);

}
