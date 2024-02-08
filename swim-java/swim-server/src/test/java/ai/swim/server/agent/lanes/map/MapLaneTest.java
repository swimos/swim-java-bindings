package ai.swim.server.agent.lanes.map;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TaggedLaneResponse;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.map.MapLane;
import ai.swim.server.lanes.map.MapLaneModel;
import ai.swim.server.lanes.map.MapLaneView;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import static ai.swim.server.agent.AgentFixture.encodeIter;
import static ai.swim.server.agent.NativeTest.runAgent;
import static ai.swim.server.lanes.Lanes.mapLane;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapLaneTest {

  @BeforeAll
  static void enterContext() {
    CallContext.enter();
  }

  private static <K, V> void dispatch(MapLaneModel<K, V> model,
      Encoder<MapOperation<K, V>> encoder,
      List<MapOperation<K, V>> expectedEvents,
      Deque<MapOperation<String, Integer>> actualEvents) {
    ByteWriter writer = new ByteWriter();

    encodeIter(writer, expectedEvents, encoder);
    model.dispatch(ReadBuffer.fromArray(writer.reader().getArray()));

    for (MapOperation<K, V> event : expectedEvents) {
      assertEquals(event, actualEvents.poll());
    }
  }

  @Test
  void mapLifecycles() {
    Deque<MapOperation<String, Integer>> events = new ArrayDeque<>();
    MapLaneView<String, Integer> view = mapLane(String.class, Integer.class)
        .onClear(() -> events.addLast(MapOperation.clear()))
        .onRemove(((key, value) -> events.addLast(MapOperation.remove(key))))
        .onUpdate(((key, oldValue, newValue) -> events.addLast(MapOperation.update(key, newValue))));

    MapLaneModel<String, Integer> model = new MapLaneModel<>(0, view, new StateCollector());
    Encoder<MapOperation<String, Integer>> encoder = new MapOperationEncoder<>(
        Form.forClass(String.class),
        Form.forClass(Integer.class));

    dispatch(model, encoder, List.of(MapOperation.update("a", 1)), events);
    dispatch(model, encoder, List.of(MapOperation.clear()), events);
    dispatch(model, encoder, List.of(MapOperation.remove("b")), events);

    dispatch(model, encoder, List.of(
        MapOperation.update("a", 1),
        MapOperation.update("b", 2),
        MapOperation.update("c", 3),
        MapOperation.remove("d"),
        MapOperation.clear()), events);
  }

  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final MapLane<String, Integer> mapLane = mapLane(String.class, Integer.class).onClear(() -> {

    }).onRemove(((key, value) -> {

    })).onUpdate(((key, oldValue, newValue) -> {

    }));

    private TestAgent(AgentContext context) {
      super(context);
    }
  }

  @Test
  void nativeTest() throws SwimServerException, IOException, NoSuchMethodException {
    Encoder<MapOperation<String, Integer>> encoder = new MapOperationEncoder<>(
        Form.forClass(String.class),
        Form.forClass(Integer.class));
    runAgent(
        TestAgent.class,
        List.of(
            TaggedLaneRequest.map("mapLane", LaneRequest.command(MapOperation.update("a", 1))),
            TaggedLaneRequest.map("mapLane", LaneRequest.command(MapOperation.update("b", 2))),
            TaggedLaneRequest.map("mapLane", LaneRequest.command(MapOperation.update("c", 3))),
            TaggedLaneRequest.map("mapLane", LaneRequest.command(MapOperation.remove("c"))),
            TaggedLaneRequest.map("mapLane", LaneRequest.command(MapOperation.clear()))),
        List.of(
            TaggedLaneResponse.map("mapLane", LaneResponse.event(MapOperation.update("a", 1))),
            TaggedLaneResponse.map("mapLane", LaneResponse.event(MapOperation.update("b", 2))),
            TaggedLaneResponse.map("mapLane", LaneResponse.event(MapOperation.update("c", 3))),
            TaggedLaneResponse.map("mapLane", LaneResponse.event(MapOperation.remove("c"))),
            TaggedLaneResponse.map("mapLane", LaneResponse.event(MapOperation.clear()))),
        encoder, true);
  }

}
