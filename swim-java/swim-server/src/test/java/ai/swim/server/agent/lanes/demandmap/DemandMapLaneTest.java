package ai.swim.server.agent.lanes.demandmap;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFixture;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TaggedLaneRequestEncoder;
import ai.swim.server.agent.TaggedLaneResponse;
import ai.swim.server.agent.TaggedLaneResponseEncoder;
import ai.swim.server.agent.TestLaneServer;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.demandmap.DemandMapLane;
import ai.swim.server.lanes.map.MapOperation;
import ai.swim.server.lanes.map.codec.MapOperationEncoder;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.structure.Form;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static ai.swim.server.lanes.Lanes.demandMapLane;
import static ai.swim.server.lanes.Lanes.valueLane;

class DemandMapLaneTest {
  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    private final Map<String, Integer> lut = new HashMap<>(Map.of("1", 1, "2", 2, "3", 3));
    @Transient
    @SwimLane
    private final DemandMapLane<String, Integer> demandMapLane = demandMapLane(String.class, Integer.class)
        .onCueKey(lut::get)
        .onSyncKeys(() -> lut.keySet().iterator());

    @Transient
    @SwimLane
    private final ValueLane<Integer> valueLane = valueLane(Integer.class).onEvent((ev) -> {
      if (ev == 1) {
        lut.put("4", 4);
        demandMapLane.cueKey("4");
      } else if (ev == 2) {
        demandMapLane.cueKey("5");
      } else {
        demandMapLane.cueKey("1");
        demandMapLane.cueKey("2");
        demandMapLane.cueKey("3");
        demandMapLane.cueKey("4");
      }
    });

    private TestAgent(AgentContext context) {
      super(context);
    }
  }

  @SwimPlane("test")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("agent")
    private TestAgent testAgent;
  }

  @Test
  void cuesKeys() throws SwimServerException, IOException {
    Encoder<Integer> integerEncoder = AgentFixture::writeIntString;
    List<TaggedLaneRequest<Integer>> requests = List.of(TaggedLaneRequest.value("valueLane", LaneRequest.command(1)),
                                                        TaggedLaneRequest.value("valueLane", LaneRequest.command(2)),
                                                        TaggedLaneRequest.value("valueLane", LaneRequest.command(3)));
    ByteWriter requestBytes = AgentFixture.encodeIter(requests, new TaggedLaneRequestEncoder<>(integerEncoder));

    ByteWriter responseBytes = new ByteWriter();
    Encoder<TaggedLaneResponse<Integer>> valueResponseEncoder = new TaggedLaneResponseEncoder<>(integerEncoder);
    Encoder<TaggedLaneResponse<MapOperation<String, Integer>>> mapResponseEncoder = new TaggedLaneResponseEncoder<>(new MapOperationEncoder<>(Form.forClass(String.class),
                                                                                                                                              Form.forClass(
                                                                                                                                                  Integer.class)));
    valueResponseEncoder.encode(TaggedLaneResponse.value("valueLane", LaneResponse.event(1)), responseBytes);
    mapResponseEncoder.encode(TaggedLaneResponse.map("demandMapLane", LaneResponse.event(MapOperation.update("4", 4))),
                              responseBytes);

    valueResponseEncoder.encode(TaggedLaneResponse.value("valueLane", LaneResponse.event(2)), responseBytes);
    mapResponseEncoder.encode(TaggedLaneResponse.map("demandMapLane", LaneResponse.event(MapOperation.remove("5"))),
                              responseBytes);

    valueResponseEncoder.encode(TaggedLaneResponse.value("valueLane", LaneResponse.event(3)), responseBytes);
    mapResponseEncoder.encode(TaggedLaneResponse.map("demandMapLane", LaneResponse.event(MapOperation.update("1", 1))),
                              responseBytes);
    mapResponseEncoder.encode(TaggedLaneResponse.map("demandMapLane", LaneResponse.event(MapOperation.update("2", 2))),
                              responseBytes);
    mapResponseEncoder.encode(TaggedLaneResponse.map("demandMapLane", LaneResponse.event(MapOperation.update("3", 3))),
                              responseBytes);
    mapResponseEncoder.encode(TaggedLaneResponse.map("demandMapLane", LaneResponse.event(MapOperation.update("4", 4))),
                              responseBytes);

    TestLaneServer.build(TestPlane.class, requestBytes, responseBytes).run();
  }

  @Test
  void syncs() throws SwimServerException, IOException {
    UUID remote = UUID.randomUUID();
    Encoder<MapOperation<String, Integer>> encoder = new MapOperationEncoder<>(Form.forClass(String.class),
                                                                               Form.forClass(Integer.class));
    TestLaneServer
        .build(TestPlane.class, List.of(TaggedLaneRequest.map("demandMapLane", LaneRequest.sync(remote))), List.of(
            TaggedLaneResponse.map("demandMapLane", LaneResponse.syncEvent(remote, MapOperation.update("1", 1))),
            TaggedLaneResponse.map("demandMapLane", LaneResponse.syncEvent(remote, MapOperation.update("2", 2))),
            TaggedLaneResponse.map("demandMapLane", LaneResponse.syncEvent(remote, MapOperation.update("3", 3))),
            TaggedLaneResponse.map("demandMapLane", LaneResponse.synced(remote))), encoder)
        .runServer();
  }
}