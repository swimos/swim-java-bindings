package ai.swim.server.schema;

import ai.swim.server.agent.Agent;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.value.ValueLane;
import org.msgpack.core.MessageBufferPacker;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AgentSchema<A extends Agent> {
  private final Class<A> clazz;
  private final String name;
  private final Map<String, LaneSchema> laneSchemas;

  public AgentSchema(Class<A> clazz, String name, Map<String, LaneSchema> laneSchemas) {
    this.clazz = clazz;
    this.name = name;
    this.laneSchemas = laneSchemas;
  }

  public static <A extends Agent> AgentSchema<A> reflectSchema(Class<A> agentClass) {
    if (agentClass == null) {
      throw new NullPointerException();
    }

    SwimAgent agentAnno = agentClass.getAnnotation(SwimAgent.class);
    if (agentAnno == null) {
      throw new IllegalArgumentException(String.format(
          "%s is not annotated with %s",
          agentClass.getCanonicalName(),
          SwimAgent.class.getName()));
    }

    String agentUri = Objects.requireNonNullElse(agentAnno.value(), agentClass.getSimpleName());
    Map<String, LaneSchema> laneSchemas = reflectLanes(agentClass);

    return new AgentSchema<>(agentClass, agentUri, laneSchemas);
  }

  private static <A extends Agent> Map<String, LaneSchema> reflectLanes(Class<A> agentClass) {
    Map<String, LaneSchema> laneSchemas = new HashMap<>();
    Field[] fields = agentClass.getDeclaredFields();

    int ids = 0;

    for (Field field : fields) {
      if (Lane.class.isAssignableFrom(field.getType())) {
        SwimLane anno = field.getAnnotation(SwimLane.class);
        if (anno != null) {
          String laneUri = Objects.requireNonNullElse(anno.value(), field.getName());
          boolean isTransient = field.getAnnotation(Transient.class) != null;
          Type type = field.getGenericType();

          if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();

            if (rawType instanceof Class<?>) {
              laneSchemas.put(laneUri, reflectLane(agentClass, (Class<?>) rawType, isTransient, ids++));
            } else {
              throw unsupportedLaneType(type, agentClass);
            }
          } else {
            throw unsupportedLaneType(type, agentClass);
          }
        }
      }
    }

    return laneSchemas;
  }

  private static <A extends Agent> IllegalArgumentException unsupportedLaneType(Type type, Class<A> agentClass) {
    return new IllegalArgumentException("Unsupported lane type: " + type + " in " + agentClass.getCanonicalName());
  }

  private static <A extends Agent> LaneSchema reflectLane(Class<A> agentClass,
      Class<?> type,
      boolean isTransient,
      int laneId) {
    if (ValueLane.class.equals(type)) {
      return new LaneSchema(isTransient, LaneKind.Value, laneId);
    } else {
      throw unsupportedLaneType(type, agentClass);
    }
  }

  public Map<String, LaneSchema> getLaneSchemas() {
    return laneSchemas;
  }

  public Class<A> getAgentClass() {
    return clazz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AgentSchema<?> that = (AgentSchema<?>) o;
    return Objects.equals(clazz, that.clazz) && Objects.equals(
        name,
        that.name) && Objects.equals(
        laneSchemas,
        that.laneSchemas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, name, laneSchemas);
  }

  @Override
  public String toString() {
    return "AgentSchema{" +
        "clazz=" + clazz +
        ", name='" + name + '\'' +
        ", laneSchemas=" + laneSchemas +
        '}';
  }

  public void pack(MessageBufferPacker packer) throws IOException {
    packer.packArrayHeader(2);
    packer.packString(name);
    packer.packMapHeader(laneSchemas.size());

    for (Map.Entry<String, LaneSchema> entry : laneSchemas.entrySet()) {
      packer.packString(entry.getKey());
      entry.getValue().pack(packer);
    }
  }

  public Map<String, Integer> laneMappings() {
    return laneSchemas
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getLaneId()));
  }
}
