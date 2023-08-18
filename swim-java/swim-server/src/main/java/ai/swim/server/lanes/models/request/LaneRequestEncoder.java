package ai.swim.server.lanes.models.request;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;

public class LaneRequestEncoder<T> implements Encoder<LaneRequest<T>> {
  private final Encoder<T> delegate;

  public LaneRequestEncoder(Encoder<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void encode(LaneRequest<T> target, Bytes dst) {
    target.encode(delegate, dst);
  }
}
