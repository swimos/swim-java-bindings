package ai.swim.server.lanes;

import ai.swim.server.agent.AgentView;
import ai.swim.structure.recognizer.RecognizerException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Model for {@link AgentView} to interact with lane implementations.
 * <p>
 * Method invocations to implementors will generally be driven by the Rust runtime.
 */
public abstract class LaneModel implements Lane {
  /**
   * Dispatch data to a lane.
   * <p>
   * Note: the data contained in {@code buffer} will only be available for the duration of the method call if the
   * invocation was triggered by the Rust runtime.
   *
   * @param buffer to dispatch
   * @throws RecognizerException if this lane failed to decode and read the buffer.
   */
  public abstract void dispatch(ByteBuffer buffer) throws RecognizerException;

  /**
   * Notify this lane that a sync request has been made.
   *
   * @param remote the {@link UUID} of the remote.
   */
  public abstract void sync(UUID remote);

  /**
   * Initialise this lane with the data in {@code buffer}.
   * <p>
   * Note: the data contained in {@code buffer} will only be available for the duration of the method call if the
   * invocation was triggered by the Rust runtime.
   *
   * @param buffer to initialise from
   */
  public abstract void init(ByteBuffer buffer);

  /**
   * Returns the {@link LaneView} associated with this {@link LaneModel}.
   *
   * @return the {@link LaneView} associated with this {@link LaneModel}.
   */
  public abstract LaneView getLaneView();
}
