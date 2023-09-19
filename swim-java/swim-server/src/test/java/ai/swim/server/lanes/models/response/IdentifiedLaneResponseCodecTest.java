package ai.swim.server.lanes.models.response;

import ai.swim.codec.Size;
import ai.swim.codec.data.ByteReader;
import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.codec.encoder.Encoder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class IdentifiedLaneResponseCodecTest {

  @Test
  void roundTrip() throws DecoderException {
    ByteWriter writer = new ByteWriter();

    IdentifiedLaneResponseEncoder<Integer> encoder = new IdentifiedLaneResponseEncoder<>(new IntEncoder());
    encoder.encode(new IdentifiedLaneResponse<>(1, LaneResponse.event(13)), writer);

    ByteReader reader = writer.reader();
    Decoder<IdentifiedLaneResponse<Integer>> decoder = new IdentifiedLaneResponseDecoder<>(new LaneResponseDecoder<>(new IntDecoder()));
    decoder = decoder.decode(reader);

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
    public Decoder<Integer> decode(ReadBuffer buffer) {
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
    public void encode(Integer target, ByteWriter buffer) {
      buffer.writeInteger(target);
    }
  }

}