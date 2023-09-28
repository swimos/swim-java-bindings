package ai.swim.server;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.agent.AgentView;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An abstract base for implementing Swim Servers.
 * <p>
 * There are two key parts to implementing a Java to Rust Swim Server, a plane schema that provides a transformation
 * between a user-defined {@link AbstractPlane} and its msgpack representation, and the {@link AgentFactory}s used for
 * instantiating new agents; this class provides the functionality to create both.
 */
public abstract class AbstractSwimServerBuilder {
  static {
    System.loadLibrary("swim_server");
  }

  /**
   * Agent factories for the Java runtime to instantiate new instances from.
   * <p>
   * NodeUri -> Agent mapping
   */
  protected final Map<String, AgentFactory<? extends AbstractAgent>> agentFactories;
  /**
   * {@link PlaneSchema} for serializing to msgpack and sending to the Rust runtime.
   */
  protected final PlaneSchema<?> schema;

  protected AbstractSwimServerBuilder(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories,
      PlaneSchema<?> schema) {
    this.agentFactories = agentFactories;
    this.schema = schema;
  }

  /**
   * Reflects a {@link PlaneSchema} for the provided class
   *
   * @param clazz to reflect
   * @param <P>   the type of the {@link AbstractPlane}
   * @return a {@link PlaneSchema} for the provided class
   * @throws SwimServerException if the class is not well-defined
   */
  protected static <P extends AbstractPlane> PlaneSchema<P> reflectPlaneSchema(Class<P> clazz) throws SwimServerException {
    return PlaneSchema.reflectSchema(clazz);
  }

  /**
   * Builds a mapping from node URI to {@link AgentFactory}.
   *
   * @param planeSchema to reflect
   * @param <P>         the type of the plane
   * @return a mapping from node URI to {@link AgentFactory}.
   */
  public static <P extends AbstractPlane> Map<String, AgentFactory<? extends AbstractAgent>> reflectAgentFactories(
      PlaneSchema<P> planeSchema) {
    Class<? extends AbstractPlane> clazz = planeSchema.getPlaneClass();
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = new HashMap<>();

    for (Field field : clazz.getDeclaredFields()) {
      SwimRoute route = field.getAnnotation(SwimRoute.class);
      if (route != null) {
        Class<?> type = field.getType();

        if (AbstractAgent.class.isAssignableFrom(type)) {
          try {
            String nodeUri = route.value();
            agentFactories.put(nodeUri, AgentFactory.forSchema(planeSchema.schemaFor(nodeUri)));
          } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
          }
        } else {
          throw new RuntimeException(String.format(
              "Class %s contains a field '%s' which is not an agent",
              clazz.getCanonicalName(),
              field.getName()));
        }
      }
    }

    return agentFactories;
  }

  /**
   * Returns a new {@link AgentView} for the provided node URI
   *
   * @param nodeUri         to create a new {@link AgentView} for
   * @param agentContextPtr a new agent-scoped {@link AgentContext}
   * @return a new {@link AgentView} for the provided node URI
   */
  public AgentView agentFor(String nodeUri, long agentContextPtr) {
    AgentFactory<? extends AbstractAgent> agentFactory = agentFactories.get(nodeUri);
    if (agentFactory == null) {
      throw new NoSuchElementException(nodeUri); // todo
    } else {
      return agentFactory.newInstance(agentContextPtr);
    }
  }

  /**
   * Runs the Swim Server.
   *
   * @return a pointer to the native instance
   * @throws IOException if there is an issue decoding the provided msgpack buffer
   */
  protected abstract long run() throws IOException;

  /**
   * Starts and runs the Swim Server
   *
   * @return a handle to the server
   * @throws IOException if there is an issue decoding the provided msgpack buffer
   */
  public SwimServerHandle runServer() throws IOException {
    return new SwimServerHandle(run());
  }

  @Override
  public String toString() {
    return "SwimServerBuilder{" + "agentFactories=" + agentFactories + '}';
  }
}
