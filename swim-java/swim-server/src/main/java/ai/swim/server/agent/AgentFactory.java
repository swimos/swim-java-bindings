package ai.swim.server.agent;

import ai.swim.server.annotations.SwimLane;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneModel;
import ai.swim.server.lanes.value.ValueLaneView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AgentFactory<A extends Agent> {
  private final Constructor<A> constructor;

  private AgentFactory(Constructor<A> constructor) {
    this.constructor = constructor;
  }

  public static <A extends Agent> AgentFactory<A> forClass(Class<A> clazz) throws NoSuchMethodException {
    try {
      Constructor<A> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      return new AgentFactory<>(constructor);
    } catch (NoSuchMethodException e) {
      throw new NoSuchMethodException("No zero-arg constructor in Agent");
    }
  }

  private static <A extends Agent> AgentModel reflectAgent(A agent) {
    Class<? extends Agent> agentClass = agent.getClass();
    Field[] fields = agentClass.getDeclaredFields();
    Map<String, LaneModel> lanes = new HashMap<>();
    StateCollector collector = new StateCollector();

    for (Field field : fields) {
      if (Lane.class.isAssignableFrom(field.getType())) {
        SwimLane anno = field.getAnnotation(SwimLane.class);
        if (anno != null) {
          String laneUri = Objects.requireNonNullElse(anno.value(), field.getName());
          Type type = field.getGenericType();

          if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();

            if (rawType instanceof Class<?>) {
              lanes.put(laneUri, reflectLane(agent, field, (Class<?>) rawType, collector));
            } else {
              throw unsupportedLaneType(type, agentClass);
            }
          } else {
            throw unsupportedLaneType(type, agentClass);
          }
        }
      }
    }

    return new AgentModel(agent, lanes, collector);
  }

  private static <A extends Agent> IllegalArgumentException unsupportedLaneType(Type type,
      Class<? extends Agent> agentClass) {
    return new IllegalArgumentException("Unsupported lane type: " + type + " in " + agentClass.getCanonicalName());
  }

  private static LaneModel reflectLane(Agent agent, Field field, Class<?> type, StateCollector collector) {
    if (ValueLane.class.equals(type)) {
      return reflectValueLane(agent, field, collector);
    } else {
      throw unsupportedLaneType(type, agent.getClass());
    }
  }

  private static LaneModel reflectValueLane(Agent agent, Field field, StateCollector collector) {
    field.setAccessible(true);
    try {
      ValueLaneView<?> laneView = (ValueLaneView<?>) field.get(agent);
      ValueLaneModel<?> model = new ValueLaneModel<>(laneView, collector);
      laneView.setModel(new ValueLaneModel<>(laneView, collector));
      return model;
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public AgentModel newInstance() {
    try {
      constructor.setAccessible(true);
      A agent = constructor.newInstance();
      return reflectAgent(agent);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Failed to create new agent", e);
    }
  }

  @Override
  public String toString() {
    return "AgentFactory{" +
        "constructor=" + constructor +
        '}';
  }
}
