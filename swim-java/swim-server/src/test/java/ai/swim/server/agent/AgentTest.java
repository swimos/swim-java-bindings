package ai.swim.server.agent;

import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.SwimServerException;
import ai.swim.server.agent.context.AgentContext;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.Encoder;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
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
      AbstractPlane plane,
      byte[] planeSpec);

  private static <E> Bytes encodeIter(Iterable<E> iterator, Encoder<E> encoder) {
    Bytes requestBytes = new Bytes();

    for (E e : iterator) {
      encoder.encode(e, requestBytes);
    }

    return requestBytes;
  }

  @SuppressWarnings("unchecked")
  <P extends AbstractPlane> void runAgent(P plane,
      List<TaggedLaneRequest<Integer>> inputs,
      List<TaggedLaneResponse<Integer>> outputs) throws SwimServerException, IOException {
    PlaneSchema<P> planeSchema = (PlaneSchema<P>) PlaneSchema.reflectSchema(plane.getClass());

    byte[] schemaBytes = planeSchema.bytes();
    Bytes requestBytes = encodeIter(inputs, new TaggedLaneRequestEncoder<>((input, dst) -> dst.writeInteger(input)));
    Bytes responseBytes = encodeIter(outputs, new TaggedLaneResponseEncoder<>((input, dst) -> dst.writeInteger(input)));

    runNativeAgent(requestBytes.getArray(), responseBytes.getArray(), plane, schemaBytes);
  }

  @Test
  void agentResponses() throws SwimServerException, IOException {
    runAgent(new TestPlane(),
             List.of(new TaggedLaneRequest<>("numLane", LaneRequest.command(2))),
             List.of(new TaggedLaneResponse<>("numLane", LaneResponse.event(2)),
                     new TaggedLaneResponse<>("plusOne", LaneResponse.event(3)),
                     new TaggedLaneResponse<>("minusOne", LaneResponse.event(1))));
  }
}
