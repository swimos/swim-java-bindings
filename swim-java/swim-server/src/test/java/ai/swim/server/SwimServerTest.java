package ai.swim.server;

import ai.swim.lang.ffi.NativeLoader;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static ai.swim.server.lanes.Lanes.valueLane;

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
    TestSwimServer.forPlane(TestPlane.class).runServer();
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
    @SwimLane("laneUri2")
    private final ValueLane<Integer> lane2 = valueLane(Integer.class);

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

    private void forward(Integer ev) {
      lane2.set(ev + 1);
    }
  }

  @SwimPlane("planeName")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("nodeUri")
    TestAgent agent;
  }

}