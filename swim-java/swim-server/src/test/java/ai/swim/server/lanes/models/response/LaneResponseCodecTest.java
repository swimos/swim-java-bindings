package ai.swim.server.lanes.models.response;

import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Decoder;
import ai.swim.server.codec.DecoderException;
import ai.swim.server.codec.Encoder;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaneResponseCodecTest {

  void roundTrip(LaneResponse<Integer> response) throws DecoderException {
    Bytes buffer = new Bytes();
    response.encode(new IntEncoder(), buffer);

    Decoder<LaneResponse<Integer>> decoder = new LaneResponseDecoder<>(new IntDecoder());
    decoder = decoder.decode(buffer);

    assertTrue(decoder.isDone());
    assertEquals(response, decoder.bind());
  }

  @Test
  void testInitialized() throws DecoderException {
    roundTrip(LaneResponse.initialized());
  }

  @Test
  void testEvent() throws DecoderException {
    roundTrip(LaneResponse.event(13));
  }

  @Test
  void testSync() throws DecoderException {
    roundTrip(LaneResponse.syncEvent(UUID.randomUUID(), 15));
  }

  @Test
  void testSynced() throws DecoderException {
    roundTrip(LaneResponse.synced(UUID.randomUUID()));
  }

  @Test
  void testMany() throws DecoderException {
    UUID uuid = UUID.randomUUID();
    List<LaneResponse<Integer>> events = List.of(
        LaneResponse.initialized(),
        LaneResponse.syncEvent(uuid, 1),
        LaneResponse.syncEvent(uuid, 2),
        LaneResponse.syncEvent(uuid, 3),
        LaneResponse.synced(uuid),
        LaneResponse.event(4),
        LaneResponse.event(5));

    Bytes buffer = new Bytes();
    Encoder<LaneResponse<Integer>> encoder = new LaneResponseEncoder<>(new IntEncoder());
    Decoder<LaneResponse<Integer>> decoder = new LaneResponseDecoder<>(new IntDecoder());

    for (LaneResponse<Integer> event : events) {
      encoder.encode(event, buffer);
    }

    for (LaneResponse<Integer> event : events) {
      decoder = decoder.decode(buffer);

      assertTrue(decoder.isDone());
      assertEquals(event, decoder.bind());

      decoder = decoder.reset();
    }
  }

  private static class IntEncoder implements Encoder<Integer> {
    @Override
    public void encode(Integer target, Bytes dst) {
      dst.writeInteger(target);
    }
  }

  private static class IntDecoder extends Decoder<Integer> {
    @Override
    public Decoder<Integer> decode(Bytes buffer) {
      return Decoder.done(this, buffer.getInteger());
    }

    @Override
    public Decoder<Integer> reset() {
      return this;
    }
  }

}