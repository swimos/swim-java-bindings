package ai.swim.server.agent;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.models.response.LaneResponseEncoder;
import java.nio.charset.StandardCharsets;

public class TaggedLaneResponseEncoder<T> implements Encoder<TaggedLaneResponse<T>> {
  private final Encoder<LaneResponse<T>> delegate;

  public TaggedLaneResponseEncoder(Encoder<T> delegate) {
    this.delegate = new LaneResponseEncoder<>(delegate);
  }

  @Override
  public void encode(TaggedLaneResponse<T> target, Bytes buffer) {
    byte[] bytes = target.getLaneUri().getBytes(StandardCharsets.UTF_8);
    buffer.writeInteger(bytes.length);
    buffer.writeByteArray(bytes);
    delegate.encode(target.getResponse(), buffer);
  }
}
