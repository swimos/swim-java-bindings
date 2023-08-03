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

public class PlaneSchema {
  private final String name;
  private final Map<String, AgentSchema> agentSchemas;

  public PlaneSchema(String name, Map<String, AgentSchema> agentSchemas) {
    this.name = name;
    this.agentSchemas = agentSchemas;
  }

  public static <P extends AbstractPlane> PlaneSchema reflectSchema(Class<P> planeClass) {
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
    Map<String, AgentSchema> agentSchemas = reflectAgents(planeClass);

    return new PlaneSchema(planeName, agentSchemas);
  }

  private static <P extends AbstractPlane> Map<String, AgentSchema> reflectAgents(Class<P> planeClass) {
    Map<String, AgentSchema> agentSchemas = new HashMap<>();
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
        "name='" + name + '\'' +
        ", agentSchemas=" + agentSchemas +
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
    PlaneSchema schema = (PlaneSchema) o;
    return Objects.equals(name, schema.name) && Objects.equals(agentSchemas, schema.agentSchemas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, agentSchemas);
  }

  public byte[] bytes() throws IOException {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packArrayHeader(2);
      packer.packString(name);
      packer.packMapHeader(agentSchemas.size());

      for (Map.Entry<String, AgentSchema> entry : agentSchemas.entrySet()) {
        packer.packString(entry.getKey());
        entry.getValue().pack(packer);
      }

      return packer.toByteArray();
    }
  }
}
