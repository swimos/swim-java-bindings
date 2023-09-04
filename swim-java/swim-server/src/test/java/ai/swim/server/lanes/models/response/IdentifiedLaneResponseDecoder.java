package ai.swim.server.lanes.models.response;

import ai.swim.codec.Size;
import ai.swim.codec.data.ByteReader;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;

public class IdentifiedLaneResponseDecoder<T> extends Decoder<IdentifiedLaneResponse<T>> {
  enum State {
    LaneId,
    Delegated
  }

  private Decoder<LaneResponse<T>> delegate;
  private State state;
  private int laneId;

  public IdentifiedLaneResponseDecoder(Decoder<LaneResponse<T>> delegate) {
    this.delegate = delegate;
    this.state = State.LaneId;
  }

  @Override
  public Decoder<IdentifiedLaneResponse<T>> decode(ByteReader buffer) throws DecoderException {
    while (true) {
      switch (state) {
        case LaneId:
          if (buffer.remaining() >= Size.INT) {
            laneId = buffer.getInteger();
            // Discard the length as it is only used when writing to Rust.
            buffer.getInteger();
            state = State.Delegated;
            break;
          } else {
            return this;
          }
        case Delegated:
          delegate = delegate.decode(buffer);
          if (delegate.isDone()) {
            return Decoder.done(this, new IdentifiedLaneResponse<>(laneId, delegate.bind()));
          } else {
            return this;
          }
      }
    }
  }

  @Override
  public Decoder<IdentifiedLaneResponse<T>> reset() {
    return new IdentifiedLaneResponseDecoder<>(delegate.reset());
  }
}
