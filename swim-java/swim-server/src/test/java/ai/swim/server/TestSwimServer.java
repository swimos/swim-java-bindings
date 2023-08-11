package ai.swim.server;

import ai.swim.server.agent.Agent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.io.IOException;
import java.util.Map;

class TestSwimServer extends AbstractSwimServerBuilder {
  public TestSwimServer(Map<String, AgentFactory<? extends Agent>> agentFactories,
      PlaneSchema schema) {
    super(agentFactories, schema);
  }

  public static <P extends AbstractPlane> TestSwimServer forPlane(Class<P> planeClass) {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends Agent>> agentFactories = reflectAgentFactories(planeSchema);
    return new TestSwimServer(agentFactories, planeSchema);
  }

  private static native void runNative(byte[] config, AbstractSwimServerBuilder plane);

  @Override
  protected long run() throws IOException {
    runNative(schema.bytes(), this);
    return 0;
  }
}
