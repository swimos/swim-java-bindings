package ai.swim.server;

import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.context.AgentContext;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.agent.AgentView;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Decoder;
import ai.swim.server.codec.DecoderException;
import ai.swim.server.codec.Size;
import ai.swim.server.lanes.models.response.IdentifiedLaneResponse;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.models.response.LaneResponseDecoder;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import static ai.swim.server.AbstractSwimServerBuilder.reflectAgentFactories;
import static ai.swim.server.AbstractSwimServerBuilder.reflectPlaneSchema;
import static ai.swim.server.lanes.Lanes.valueLane;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwimServerTest {

  static {
    try {
      NativeLoader.loadLibraries("swim_server_test");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static native void forPlane(byte[] config);

  @Test
  void testCodec() throws IOException, SwimServerException {
    PlaneSchema<TestPlane> planeSchema = PlaneSchema.reflectSchema(TestPlane.class);
    forPlane(planeSchema.bytes());
  }

  @Test
  void testRun() throws IOException, SwimServerException {
    MockSwimServer.forPlane(TestPlane.class).runServer();
  }

  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane("laneUri")
    private final ValueLane<Integer> lane = valueLane(Integer.class).onEvent((ev) -> {
      System.out.println("Java agent on event: " + ev);
      forward(ev);
    }).onSet(((oldValue, newValue) -> {
      System.out.println("Java agent on set. Old: " + oldValue + ", new: " + newValue);
    }));

    @Transient
    @SwimLane
    private final ValueLane<Integer> plusOne = valueLane(Integer.class);

    @Transient
    @SwimLane
    private final ValueLane<Integer> minusOne = valueLane(Integer.class);

    private TestAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      System.out.println("Did start");
    }

    @Override
    public void didStop() {
      System.out.println("Did stop");
    }

    private void forward(int ev) {
      plusOne.set(ev + 1);
      minusOne.set(ev - 1);

      if (ev == 13) {
        ValueLaneView<Integer> dynamic = valueLane(Integer.class).onEvent(value -> System.out.println("Dynamic"));
        getContext().openLane(dynamic, "mock", false);
      }
    }
  }

  @SwimPlane("planeName")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("nodeUri")
    TestAgent agent;
  }

  private static final class StringDecoder extends Decoder<String> {
    @Override
    public Decoder<String> decode(Bytes buffer) {
      if (buffer.remaining() >= Size.LONG) {
        long len = buffer.getLong();
        if (buffer.remaining() >= len) {
          byte[] stringBuf = new byte[Math.toIntExact(len)];
          buffer.getByteArray(stringBuf);
          return Decoder.done(this, new String(stringBuf));
        } else {
          return this;
        }
      } else {
        return this;
      }
    }

    @Override
    public Decoder<String> reset() {
      return this;
    }
  }

  private static final class IdentifiedLaneResponseDecoder extends Decoder<IdentifiedLaneResponse<String>> {
    @Override
    public Decoder<IdentifiedLaneResponse<String>> decode(Bytes buffer) throws DecoderException {
      if (buffer.remaining() == 0) {
        return this;
      }

      buffer.getInteger(); // discard len
      int laneId = buffer.getInteger();

      LaneResponseDecoder<String> delegate = new LaneResponseDecoder<>(new StringDecoder());
      LaneResponse<String> response = delegate.decode(buffer).bind();

      return Decoder.done(this, new IdentifiedLaneResponse<>(laneId, response));
    }

    @Override
    public Decoder<IdentifiedLaneResponse<String>> reset() {
      return this;
    }
  }

  /**
   * Tests that the agent sets the state of the other lanes correctly and that the responses are encoded correctly.
   */
  @Test
  void encodesResponses() throws SwimServerException, DecoderException {
    PlaneSchema<TestPlane> planeSchema = reflectPlaneSchema(TestPlane.class);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);

    AgentFactory<? extends AbstractAgent> agentFactory = agentFactories.get("nodeUri");
    int laneId = agentFactory.idFor("laneUri");
    int plusId = agentFactory.idFor("plusOne");
    int minusId = agentFactory.idFor("minusOne");

    AgentView agentView = agentFactory.newInstance(0);
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {49, 51}); // 13

    Bytes bytes = new Bytes();
    bytes.writeByteArray(agentView.dispatch(laneId, buffer));

    // assert that there isn't any more data available. only three events should have been written and these would fit
    // in the buffer
    assertEquals(0, bytes.getByte());

    Decoder<IdentifiedLaneResponse<String>> decoder = new IdentifiedLaneResponseDecoder();

    HashSet<IdentifiedLaneResponse<String>> expected = new HashSet<>();
    expected.add(new IdentifiedLaneResponse<>(laneId, LaneResponse.event("13")));
    expected.add(new IdentifiedLaneResponse<>(plusId, LaneResponse.event("14")));
    expected.add(new IdentifiedLaneResponse<>(minusId, LaneResponse.event("12")));

    while (true) {
      decoder = decoder.decode(bytes);
      if (decoder.isDone()) {
        expected.remove(decoder.bind());
        decoder = decoder.reset();
      } else if (bytes.remaining() == 0) {
        break;
      } else {
        throw new IllegalStateException("Unconsumed input");
      }
    }

    assertTrue(expected.isEmpty());
  }

}