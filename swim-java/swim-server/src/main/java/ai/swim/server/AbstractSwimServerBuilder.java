package ai.swim.server;

import ai.swim.server.agent.Agent;
import ai.swim.server.agent.AgentFactory;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.plane.AbstractPlane;
import ai.swim.server.schema.PlaneSchema;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class AbstractSwimServerBuilder {
  static {
    System.loadLibrary("swim_server");
  }

  protected final Map<String, AgentFactory<? extends Agent>> agentFactories;
  protected final PlaneSchema schema;

  public AbstractSwimServerBuilder(Map<String, AgentFactory<? extends Agent>> agentFactories, PlaneSchema schema) {
    this.agentFactories = agentFactories;
    this.schema = schema;
  }

  protected static <P extends AbstractPlane> Map<String, AgentFactory<? extends Agent>> reflectAgentFactories(Class<P> clazz) {
    P plane;

    try {
      Constructor<P> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);

      plane = constructor.newInstance();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Class does not contain a zero-arg constructor", e);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Failed to create new plane", e);
    }

    return reflectAgentFactories(plane);
  }

  protected static <P extends AbstractPlane> PlaneSchema reflectPlaneSchema(Class<P> clazz) {
    return PlaneSchema.reflectSchema(clazz);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, AgentFactory<? extends Agent>> reflectAgentFactories(AbstractPlane plane) {
    Class<? extends AbstractPlane> clazz = plane.getClass();
    Map<String, AgentFactory<? extends Agent>> agentFactories = new HashMap<>();

    for (Field field : clazz.getDeclaredFields()) {
      SwimRoute route = field.getAnnotation(SwimRoute.class);
      if (route != null) {
        Class<?> type = field.getType();

        if (Agent.class.isAssignableFrom(type)) {
          Class<? extends Agent> agentClass = (Class<? extends Agent>) type;
          try {
            agentFactories.put(route.value(), AgentFactory.forClass(agentClass));
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

  public Agent agentFor(String uri) {
    AgentFactory<? extends Agent> agentFactory = agentFactories.get(uri);
    if (agentFactory == null) {
      throw new NoSuchElementException(uri); // todo
    } else {
      return agentFactory.newInstance();
    }
  }

  public AbstractSwimServerBuilder setConfig() {
    throw new AssertionError();
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
