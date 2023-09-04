package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.AbstractSwimServerBuilder;
import ai.swim.server.SwimServerException;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static ai.swim.server.lanes.Lanes.valueLane;

class AgentTest {

  static {
    try {
      NativeLoader.loadLibraries("swim_server_test");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @SwimAgent
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class).onEvent(this::forward);

    @Transient
    @SwimLane
    private final ValueLane<Integer> plusOne = valueLane(Integer.class);

    @Transient
    @SwimLane
    private final ValueLane<Integer> minusOne = valueLane(Integer.class);

    private TestAgent(AgentContext context) {
      super(context);
    }

    private void forward(int ev) {
      plusOne.set(ev + 1);
      minusOne.set(ev - 1);
    }
  }

  @SwimPlane("mock")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("agent")
    private TestAgent agent;
  }

  private static native void runNativeAgent(byte[] inputs,
      byte[] expectedResponses,
      AbstractSwimServerBuilder server,
      byte[] planeSpec);

  private static <E> ByteWriter encodeIter(Iterable<E> iterator, Encoder<E> encoder) {
    ByteWriter requestBytes = new ByteWriter();

    for (E e : iterator) {
      encoder.encode(e, requestBytes);
    }

    return requestBytes;
  }

  private static class TestServer extends AbstractSwimServerBuilder {

    private final ByteWriter responseBytes;
    private final ByteWriter requestBytes;

    protected TestServer(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories,
        PlaneSchema<?> schema,
        ByteWriter requestBytes,
        ByteWriter responseBytes) {
      super(agentFactories, schema);
      this.requestBytes = requestBytes;
      this.responseBytes = responseBytes;
    }

    public static <P extends AbstractPlane> TestServer test(Class<P> planeClass,
        List<TaggedLaneRequest<Integer>> inputs,
        List<TaggedLaneResponse<Integer>> outputs) throws SwimServerException {
      PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
      Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);

      ByteWriter requestBytes = encodeIter(inputs, new TaggedLaneRequestEncoder<>(AgentTest::writeInt));
      ByteWriter responseBytes = encodeIter(outputs, new TaggedLaneResponseEncoder<>(AgentTest::writeInt));

      return new TestServer(agentFactories, planeSchema, requestBytes, responseBytes);
    }

    @Override
    protected long run() throws IOException {
      runNativeAgent(requestBytes.getArray(), responseBytes.getArray(), this, schema.bytes());
      return 0;
    }
  }

  private static void writeInt(int v, ByteWriter into) {
    byte[] bytes = Integer.toString(v).getBytes(StandardCharsets.UTF_8);
    into.writeLong(bytes.length);
    into.writeByteArray(bytes);
  }

  @Test
  void agentResponses() throws SwimServerException, IOException {
    TestServer
        .test(
            TestPlane.class,
            List.of(new TaggedLaneRequest<>("numLane", LaneRequest.command(2))),
            List.of(
                new TaggedLaneResponse<>("numLane", LaneResponse.event(2)),
                new TaggedLaneResponse<>("plusOne", LaneResponse.event(3)),
                new TaggedLaneResponse<>("minusOne", LaneResponse.event(1))))
        .run();
  }

  @SwimAgent
  private static class DynamicTestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class).onEvent(this::onEvent);

    private DynamicTestAgent(AgentContext context) {
      super(context);
    }

    private void onEvent(int ev) {
      if (ev == 13) {
        ValueLaneView<Integer> lane = valueLane(Integer.class);
        getContext().openLane(lane, "dynamic", true);
        lane.set(15);
      } else if (ev == 14) {
        @SuppressWarnings("unchecked") ValueLaneView<Integer> lane = (ValueLaneView<Integer>) getContext().laneFor(
            "dynamic");
        lane.set(16);
      }
    }
  }

  @SwimPlane("mock")
  private static class DynamicTestPlane extends AbstractPlane {
    @SwimRoute("agent")
    private DynamicTestAgent agent;
  }

  @Test
  void dynamicLaneOpens() throws SwimServerException, IOException {
    TestServer.test(
        DynamicTestPlane.class,
        List.of(
            new TaggedLaneRequest<>("numLane", LaneRequest.command(13)),
            new TaggedLaneRequest<>("numLane", LaneRequest.command(14))),
        List.of(
            new TaggedLaneResponse<>("numLane", LaneResponse.event(13)),
            new TaggedLaneResponse<>("dynamic", LaneResponse.event(15)),
            new TaggedLaneResponse<>("numLane", LaneResponse.event(14)),
            new TaggedLaneResponse<>("dynamic", LaneResponse.event(16)))).run();
  }
}
