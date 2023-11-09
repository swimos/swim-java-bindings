package ai.swim.server;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.io.IOException;
import java.util.Map;

class MockSwimServer extends AbstractSwimServerBuilder {
  private MockSwimServer(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories,
      PlaneSchema<? extends AbstractPlane> schema) {
    super(agentFactories, schema);
  }

  public static <P extends AbstractPlane> MockSwimServer forPlane(Class<P> planeClass) throws SwimServerException {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);
    return new MockSwimServer(agentFactories, planeSchema);
  }

  private static native void runNative(byte[] config, AbstractSwimServerBuilder plane);

  @Override
  protected long run() throws IOException {
    runNative(schema.bytes(), this);
    return 0;
  }
}
