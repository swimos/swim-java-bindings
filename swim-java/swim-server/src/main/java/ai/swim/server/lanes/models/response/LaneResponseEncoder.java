package ai.swim.server.lanes.models.response;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;

/**
 * A encoder for encoding {@link LaneResponse}'s.
 *
 * @param <T> the responses event type.
 */
public class LaneResponseEncoder<T> implements Encoder<LaneResponse<T>> {
  private final Encoder<T> delegate;

  public LaneResponseEncoder(Encoder<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void encode(LaneResponse<T> target, Bytes dst) {
    target.encode(delegate, dst);
  }
}
