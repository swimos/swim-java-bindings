package ai.swim.server.agent.schema;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.value.ValueLane;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AgentSpec {
  private final String agentUri;
  private final Constructor<? extends AbstractAgent> constructor;
  private final Map<String, LaneSpec> laneSpecs;

  public AgentSpec(String agentUri,
      Constructor<? extends AbstractAgent> constructor,
      Map<String, LaneSpec> laneSpecs) {
    this.agentUri = agentUri;
    this.constructor = constructor;
    this.laneSpecs = laneSpecs;
  }

  public static AgentSpec reflectSchema(AbstractAgent agent) throws NoSuchMethodException {
    if (agent == null) {
      throw new NullPointerException();
    }

    Class<? extends AbstractAgent> agentClass = agent.getClass();

    SwimAgent agentAnno = agentClass.getAnnotation(SwimAgent.class);
    if (agentAnno == null) {
      throw new IllegalArgumentException(String.format(
          "%s is not annotated with %s",
          agentClass.getCanonicalName(),
          SwimAgent.class.getName()));
    }

    String agentUri = Objects.requireNonNullElse(agentAnno.value(), agentClass.getSimpleName());

    Constructor<? extends AbstractAgent> constructor;

    try {
      constructor = agentClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new NoSuchMethodException("No zero-arg constructor in Agent");
    }

    Map<String, LaneSpec> laneSpecs = reflectLanes(agentClass);

    return new AgentSpec(agentUri, constructor, laneSpecs);
  }

  private static Map<String, LaneSpec> reflectLanes(Class<? extends AbstractAgent> agentClass) {
    Field[] fields = agentClass.getDeclaredFields();
    Map<String, LaneSpec> laneSpecs = new HashMap<>();

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
              laneSpecs.put(laneUri, reflectLane(agentClass, (Class<?>) rawType, isTransient));
            } else {
              throw unsupportedLaneType(type, agentClass);
            }
          } else {
            throw unsupportedLaneType(type, agentClass);
          }
        }
      }
    }

    return laneSpecs;
  }

  private static IllegalArgumentException unsupportedLaneType(Type type, Class<? extends AbstractAgent> agentClass) {
    return new IllegalArgumentException("Unsupported lane type: " + type + " in " + agentClass.getCanonicalName());
  }

  private static LaneSpec reflectLane(Class<? extends AbstractAgent> agentClass, Class<?> type, boolean isTransient) {
    if (ValueLane.class.equals(type)) {
      return new LaneSpec(isTransient, LaneKind.Value);
    } else {
      throw unsupportedLaneType(type, agentClass);
    }
  }

  public Constructor<? extends AbstractAgent> getConstructor() {
    return constructor;
  }

  public Map<String, LaneSpec> getLaneSpecs() {
    return laneSpecs;
  }

  @Override
  public String toString() {
    return "AgentSpec{" +
        "agentUri='" + agentUri + '\'' +
        ", constructor=" + constructor +
        ", laneSpecs=" + laneSpecs +
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
    AgentSpec agentSpec = (AgentSpec) o;
    return Objects.equals(agentUri, agentSpec.agentUri) && Objects.equals(
        constructor,
        agentSpec.constructor) && Objects.equals(
        laneSpecs,
        agentSpec.laneSpecs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentUri, constructor, laneSpecs);
  }
}
