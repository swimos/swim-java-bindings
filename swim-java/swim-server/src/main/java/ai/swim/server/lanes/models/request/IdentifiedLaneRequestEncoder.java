package ai.swim.server.lanes.models.request;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;

public class IdentifiedLaneRequestEncoder<T> implements Encoder<IdentifiedLaneRequest<T>> {
  private final Encoder<LaneRequest<T>> delegate;

  public IdentifiedLaneRequestEncoder(Encoder<T> delegate) {
    this.delegate = new LaneRequestEncoder<>(delegate);
  }

  @Override
  public void encode(IdentifiedLaneRequest<T> target, Bytes buffer) {
    buffer.writeInteger(target.getLaneId());
    delegate.encode(target.getLaneRequest(), buffer);
  }
}
