package ai.swim.server.lanes.models.response;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Decoder;
import ai.swim.server.codec.DecoderException;
import ai.swim.server.codec.Encoder;
import ai.swim.server.codec.Size;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class IdentifiedLaneResponseCodecTest {

  @Test
  void roundTrip() throws DecoderException {
    Bytes bytes = new Bytes();

    IdentifiedLaneResponseEncoder<Integer> encoder = new IdentifiedLaneResponseEncoder<>(new IntEncoder());
    encoder.encode(new IdentifiedLaneResponse<>(1, LaneResponse.event(13)), bytes);

    Decoder<IdentifiedLaneResponse<Integer>> decoder = new IdentifiedLaneResponseDecoder<>(new LaneResponseDecoder<>(new IntDecoder()));
    decoder = decoder.decode(bytes);

    if (decoder.isDone()) {
      IdentifiedLaneResponse<Integer> response = decoder.bind();
      assertEquals(1, response.getLaneId());
      assertEquals(LaneResponse.event(13), response.getLaneResponse());
    } else {
      fail("Unconsumed input");
    }
  }

  private static class IntDecoder extends Decoder<Integer> {
    @Override
    public Decoder<Integer> decode(Bytes buffer) {
      if (buffer.remaining() >= Size.INT) {
        return Decoder.done(this, buffer.getInteger());
      } else {
        return this;
      }
    }

    @Override
    public Decoder<Integer> reset() {
      return this;
    }
  }

  private static class IntEncoder implements Encoder<Integer> {
    @Override
    public void encode(Integer target, Bytes buffer) {
      buffer.writeInteger(target);
    }
  }

}