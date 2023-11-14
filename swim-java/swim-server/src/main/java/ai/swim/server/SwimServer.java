package ai.swim.server;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.util.Map;

/**
 * Base class for building and running a new Swim Server.
 */
public class SwimServer extends AbstractSwimServerBuilder {
  private SwimServer(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories, PlaneSchema<?> schema) {
    super(agentFactories, schema);
  }

  /**
   * Builds a Swim Server instance for the provided plane.
   *
   * @param planeClass to reflect and build the Swim Server from
   * @param <P>        the type of the plane
   * @return a new Swim Server instance
   * @throws SwimServerException if the provided class is not well-defined
   */
  public static <P extends AbstractPlane> SwimServer forPlane(Class<P> planeClass) throws SwimServerException {
    PlaneSchema<P> planeSchema = reflectPlaneSchema(planeClass);
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = reflectAgentFactories(planeSchema);
    return new SwimServer(agentFactories, planeSchema);
  }

  private static native long runSwimServer();

  @Override
  protected long run() {
    return runSwimServer();
  }
}
