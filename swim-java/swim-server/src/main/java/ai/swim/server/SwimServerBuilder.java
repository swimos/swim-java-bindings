package ai.swim.server;

import ai.swim.server.agent.Agent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.util.Map;

public class SwimServerBuilder extends AbstractSwimServerBuilder {
  private SwimServerBuilder(Map<String, AgentFactory<? extends Agent>> agentFactories, PlaneSchema<?> schema) {
    super(agentFactories, schema);
  }

  public static <P extends AbstractPlane> SwimServerBuilder forPlane(Class<P> planeClass) {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends Agent>> agentFactories = reflectAgentFactories(planeSchema);
    return new SwimServerBuilder(agentFactories, planeSchema);
  }

  private static native long runSwimServer();

  @Override
  protected long run() {
    return runSwimServer();
  }
}
