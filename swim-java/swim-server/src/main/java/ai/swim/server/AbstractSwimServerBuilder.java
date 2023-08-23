package ai.swim.server;

import ai.swim.server.agent.AbstractAgent;
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

public abstract class AbstractSwimServerBuilder {
  static {
    System.loadLibrary("swim_server");
  }

  // NodeUri -> Agent mapping
  protected final Map<String, AgentFactory<? extends AbstractAgent>> agentFactories;
  protected final PlaneSchema<?> schema;

  protected AbstractSwimServerBuilder(Map<String, AgentFactory<? extends AbstractAgent>> agentFactories,
      PlaneSchema<?> schema) {
    this.agentFactories = agentFactories;
    this.schema = schema;
  }

  protected static <P extends AbstractPlane> PlaneSchema<P> reflectPlaneSchema(Class<P> clazz) throws SwimServerException {
    return PlaneSchema.reflectSchema(clazz);
  }

  public static <P extends AbstractPlane> Map<String, AgentFactory<? extends AbstractAgent>> reflectAgentFactories(
      PlaneSchema<P> planeSchema) {
    Class<? extends AbstractPlane> clazz = planeSchema.getPlaneClass();
    Map<String, AgentFactory<? extends AbstractAgent>> agentFactories = new HashMap<>();

    for (Field field : clazz.getDeclaredFields()) {
      SwimRoute route = field.getAnnotation(SwimRoute.class);
      if (route != null) {
        Class<?> type = field.getType();

        if (AbstractAgent.class.isAssignableFrom(type)) {
          @SuppressWarnings("unchecked") Class<? extends AbstractAgent> agentClass = (Class<? extends AbstractAgent>) type;
          try {
            agentFactories.put(route.value(), AgentFactory.forSchema(planeSchema.schemaFor(agentClass)));
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

  public AgentView agentFor(String uri, long agentContextPtr) {
    AgentFactory<? extends AbstractAgent> agentFactory = agentFactories.get(uri);
    if (agentFactory == null) {
      throw new NoSuchElementException(uri); // todo
    } else {
      return agentFactory.newInstance(agentContextPtr);
    }
  }

  protected abstract long run() throws IOException;

  public SwimServerHandle runServer() throws IOException {
    return new SwimServerHandle(run());
  }

  @Override
  public String toString() {
    return "SwimServerBuilder{" +
        "agentFactories=" + agentFactories +
        '}';
  }
}
