package ai.swim.server.lanes.models.response;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;

public class IdentifiedLaneResponseEncoder<T> implements Encoder<IdentifiedLaneResponse<T>> {
  private final Encoder<LaneResponse<T>> delegate;

  public IdentifiedLaneResponseEncoder(Encoder<T> delegate) {
    this.delegate = new LaneResponseEncoder<>(delegate);
  }

  @Override
  public void encode(IdentifiedLaneResponse<T> target, ByteWriter buffer) {
    buffer.writeInteger(target.getLaneId());
    delegate.encodeWithLen(target.getLaneResponse(), buffer);
  }
}
