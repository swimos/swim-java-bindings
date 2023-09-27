package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.SwimServerException;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.map.MapLane;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.structure.Form;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static ai.swim.server.lanes.Lanes.mapLane;
import static ai.swim.server.lanes.Lanes.valueLane;

class AgentTest {

  @Test
  void agentResponses() throws SwimServerException, IOException {
    TestLaneServer.build(TestPlane.class, List.of(TaggedLaneRequest.value("numLane", LaneRequest.command(2))), List.of(
        TaggedLaneResponse.value("numLane", LaneResponse.event(2)),
        TaggedLaneResponse.value("plusOne", LaneResponse.event(3)),
        TaggedLaneResponse.value("minusOne", LaneResponse.event(1))), AgentFixture::writeIntString).run();
  }

  @Test
  void dynamicLaneOpens() throws SwimServerException, IOException {
    TestLaneServer.build(
        DynamicTestPlane.class,
        List.of(
            TaggedLaneRequest.value("numLane", LaneRequest.command(13)),
            TaggedLaneRequest.value("numLane", LaneRequest.command(14))),
        List.of(
            TaggedLaneResponse.value("numLane", LaneResponse.event(13)),
            TaggedLaneResponse.value("dynamic", LaneResponse.event(15)),
            TaggedLaneResponse.value("numLane", LaneResponse.event(14)),
            TaggedLaneResponse.value("dynamic", LaneResponse.event(16))),
        AgentFixture::writeIntString).run();
  }

  @SwimAgent
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final ValueLane<Integer> plusOne = valueLane(Integer.class);
    @Transient
    @SwimLane
    private final ValueLane<Integer> minusOne = valueLane(Integer.class);
    @Transient
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class).onEvent(this::forward);

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

  @SwimPlane("mock")
  private static class HeterogeneousLanesPlane extends AbstractPlane {
    @SwimRoute("agent")
    private HeterogeneousLanesAgent agent;
  }

  @SwimAgent
  private static class HeterogeneousLanesAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final ValueLane<Integer> numLane = valueLane(Integer.class).onEvent(this::onEvent);
    @Transient
    @SwimLane
    private final ValueLane<Integer> updateCount = valueLane(Integer.class);

    @Transient
    @SwimLane
    private final MapLane<Integer, Boolean> parity = mapLane(
        Integer.class,
        Boolean.class).onUpdate(((key, oldValue, newValue) -> {
      Integer oldCount = updateCount.get();
      updateCount.set(oldCount == null ? 1 : oldCount + 1);
    }));

    private HeterogeneousLanesAgent(AgentContext context) {
      super(context);
    }

    private void onEvent(int ev) {
      parity.put(ev, ev % 2 == 0);
    }
  }

  @Test
  void heterogeneousLanes() throws SwimServerException, IOException {
    Encoder<Integer> integerEncoder = AgentFixture::writeIntString;

    List<TaggedLaneRequest<Integer>> requests = List.of(
        TaggedLaneRequest.value("numLane", LaneRequest.command(1)),
        TaggedLaneRequest.value("numLane", LaneRequest.command(2)));
    ByteWriter requestBytes = AgentFixture.encodeIter(requests, new TaggedLaneRequestEncoder<>(integerEncoder));

    List<TaggedLaneResponse<?>> value = List.of(
        TaggedLaneResponse.value("numLane", LaneResponse.event(1)),
        TaggedLaneResponse.map(
            "parity",
            LaneResponse.event(MapOperation.update(
                1,
                false))),
        TaggedLaneResponse.value("updateCount", LaneResponse.event(1)));

    Encoder<TaggedLaneResponse<Integer>> valueResponseEncoder = new TaggedLaneResponseEncoder<>(integerEncoder);
    Encoder<TaggedLaneResponse<MapOperation<Integer, Boolean>>> mapResponseEncoder = new TaggedLaneResponseEncoder<>(new MapOperationEncoder<>(
        Form.forClass(Integer.class),
        Form.forClass(
            Boolean.class)));

    ByteWriter responseBytes = new ByteWriter();

    valueResponseEncoder.encode(TaggedLaneResponse.value("numLane", LaneResponse.event(1)), responseBytes);
    mapResponseEncoder.encode(
        TaggedLaneResponse.map("parity", LaneResponse.event(MapOperation.update(1, false))),
        responseBytes);
    valueResponseEncoder.encode(TaggedLaneResponse.value("updateCount", LaneResponse.event(1)), responseBytes);

    valueResponseEncoder.encode(TaggedLaneResponse.value("numLane", LaneResponse.event(2)), responseBytes);
    mapResponseEncoder.encode(
        TaggedLaneResponse.map("parity", LaneResponse.event(MapOperation.update(2, true))),
        responseBytes);
    valueResponseEncoder.encode(TaggedLaneResponse.value("updateCount", LaneResponse.event(2)), responseBytes);

    TestLaneServer.build(HeterogeneousLanesPlane.class, requestBytes, responseBytes).run();
  }
}
