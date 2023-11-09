package ai.swim.server.lanes.models.response;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;

public class IdentifiedLaneResponseEncoder<T> implements Encoder<IdentifiedLaneResponse<T>> {
  private final Encoder<LaneResponse<T>> delegate;

  public IdentifiedLaneResponseEncoder(Encoder<T> delegate) {
    this.delegate = new LaneResponseEncoder<>(delegate);
  }

  @Override
  public void encode(IdentifiedLaneResponse<T> target, Bytes buffer) {
    buffer.writeInteger(target.getLaneId());
    delegate.encodeWithIntLen(target.getLaneResponse(), buffer);
  }
}
