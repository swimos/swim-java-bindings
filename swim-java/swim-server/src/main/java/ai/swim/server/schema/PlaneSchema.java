package ai.swim.server.schema;

import ai.swim.server.agent.Agent;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.plane.AbstractPlane;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlaneSchema<P extends AbstractPlane> {
  private final Class<P> planeClass;
  private final String name;
  private final Map<String, AgentSchema<?>> agentSchemas;
  private final Map<Class<? extends Agent>, String> uriResolver;

  public PlaneSchema(Class<P> planeClass, String name, Map<String, AgentSchema<?>> agentSchemas,
      Map<Class<? extends Agent>, String> uriResolver) {
    this.planeClass = planeClass;
    this.name = name;
    this.agentSchemas = agentSchemas;
    this.uriResolver = uriResolver;
  }

  public static <P extends AbstractPlane> PlaneSchema<P> reflectSchema(Class<P> planeClass) {
    if (planeClass == null) {
      throw new NullPointerException();
    }

    SwimPlane planeAnno = planeClass.getAnnotation(SwimPlane.class);
    if (planeAnno == null) {
      throw new IllegalArgumentException(String.format(
          "%s is not annotated with %s",
          planeClass.getCanonicalName(),
          SwimPlane.class.getName()));
    }

    String planeName = planeAnno.value();
    Map<String, AgentSchema<?>> agentSchemas = reflectAgents(planeClass);
    Map<Class<? extends Agent>, String> uriResolver = agentSchemas
        .entrySet()
        .stream()
        .collect(Collectors.toMap(entry -> entry.getValue().getAgentClass(), Map.Entry::getKey));

    return new PlaneSchema<>(planeClass, planeName, agentSchemas, uriResolver);
  }

  private static <P extends AbstractPlane> Map<String, AgentSchema<?>> reflectAgents(Class<P> planeClass) {
    Map<String, AgentSchema<?>> agentSchemas = new HashMap<>();
    Field[] fields = planeClass.getDeclaredFields();

    for (Field field : fields) {
      Class<?> fieldType = field.getType();
      SwimRoute routeAnno = field.getAnnotation(SwimRoute.class);

      if (routeAnno != null) {
        if (Agent.class.isAssignableFrom(fieldType)) {
          @SuppressWarnings("unchecked") Class<? extends Agent> agentClass = (Class<? extends Agent>) fieldType;
          agentSchemas.put(routeAnno.value(), AgentSchema.reflectSchema(agentClass));
        } else {
          throw new IllegalArgumentException(String.format(
              "%s is annotated with %s but its field does not extend from %s",
              field.getName(),
              SwimRoute.class.getCanonicalName(),
              Agent.class.getCanonicalName()));
        }
      }
    }

    return agentSchemas;
  }

  @Override
  public String toString() {
    return "PlaneSchema{" +
        "planeClass=" + planeClass +
        ", name='" + name + '\'' +
        ", agentSchemas=" + agentSchemas +
        ", uriResolver=" + uriResolver +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlaneSchema<?> that = (PlaneSchema<?>) o;
    return Objects.equals(planeClass, that.planeClass) && Objects.equals(
        name,
        that.name) && Objects.equals(
        agentSchemas,
        that.agentSchemas) && Objects.equals(uriResolver, that.uriResolver);
  }

  @Override
  public int hashCode() {
    return Objects.hash(planeClass, name, agentSchemas, uriResolver);
  }

  public byte[] bytes() throws IOException {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packArrayHeader(2);
      packer.packString(name);
      packer.packMapHeader(agentSchemas.size());

      for (Map.Entry<String, AgentSchema<?>> entry : agentSchemas.entrySet()) {
        packer.packString(entry.getKey());
        entry.getValue().pack(packer);
      }

      return packer.toByteArray();
    }
  }

  public AgentSchema<?> schemaFor(Class<? extends Agent> agentClass) {
    String uri = uriResolver.get(agentClass);
    return agentSchemas.get(uri);
  }

  public Class<P> getPlaneClass() {
    return planeClass;
  }
}
