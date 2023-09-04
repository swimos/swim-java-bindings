package ai.swim.server.lanes.models.request;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;

public class LaneRequestEncoder<T> implements Encoder<LaneRequest<T>> {
  private final Encoder<T> delegate;

  public LaneRequestEncoder(Encoder<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void encode(LaneRequest<T> target, ByteWriter dst) {
    target.encode(delegate, dst);
  }
}
