package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.request.LaneRequestEncoder;
import java.nio.charset.StandardCharsets;

public class TaggedLaneRequestEncoder<T> implements Encoder<TaggedLaneRequest<T>> {
  private final Encoder<LaneRequest<T>> delegate;

  public TaggedLaneRequestEncoder(Encoder<T> delegate) {
    this.delegate = new LaneRequestEncoder<>(delegate);
  }

  @Override
  public void encode(TaggedLaneRequest<T> target, ByteWriter buffer) {
    byte[] bytes = target.getLaneUri().getBytes(StandardCharsets.UTF_8);
    buffer.writeInteger(bytes.length);
    buffer.writeByteArray(bytes);
    delegate.encode(target.getRequest(), buffer);
  }
}
