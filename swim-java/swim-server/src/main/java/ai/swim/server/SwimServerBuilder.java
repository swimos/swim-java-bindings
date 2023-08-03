package ai.swim.server;

import ai.swim.server.agent.Agent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.util.Map;

public class SwimServerBuilder extends AbstractSwimServerBuilder {
  private SwimServerBuilder(Map<String, AgentFactory<? extends Agent>> agentFactories,
      PlaneSchema schema) {
    super(agentFactories, schema);
  }

  private static native long runSwimServer();

  public static <P extends AbstractPlane> SwimServerBuilder forPlane(Class<P> planeClass) {
    Map<String, AgentFactory<? extends Agent>> agentFactories = reflectAgentFactories(planeClass);
    PlaneSchema planeSchema = reflectPlaneSchema(planeClass);
    return new SwimServerBuilder(agentFactories, planeSchema);
  }

  @Override
  protected long run() {
    return runSwimServer();
  }
}
