package ai.swim.server;

import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.codec.Bytes;
import ai.swim.server.codec.WithLenReconEncoder;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.models.response.LaneResponseEncoder;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import ai.swim.structure.writer.std.ScalarWriters;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Arrays;

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
  void testCodec() throws IOException {
    PlaneSchema planeSchema = PlaneSchema.reflectSchema(TestPlane.class);
    forPlane(planeSchema.bytes());
  }

    @Test
  void testRun() throws IOException {
    TestSwimServer.forPlane(TestPlane.class).runServer();
  }

  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane("laneUri")
    private final ValueLane<Integer> lane = valueLane(Integer.class).onEvent((ev) -> {
      System.out.println("Java agent on event: " + ev);
    }).onSet(((oldValue, newValue) -> {
      System.out.println("Java agent on set. Old: " + oldValue + ", new: " + newValue);
    }));

    @Override
    public void didStart() {
      System.out.println("Did start");
    }

    @Override
    public void didStop() {
      System.out.println("Did stop");
    }
  }

  @SwimPlane("planeName")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("nodeUri")
    TestAgent agent;
  }

}