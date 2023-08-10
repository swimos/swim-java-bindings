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

/**
 * A factory for constructing agents of type {@code A}.
 * <p>
 * An initialised agent is an {@link AgentModel}s that is used to dispatch events from the Rust runtime to lanes;
 * decoding the message requests, setting the state of the lanes, invoking user-defined lifecycle events and encoding
 * responses.
 *
 * @param <A>
 */
public class AgentFactory<A extends Agent> {
  private final Constructor<A> constructor;

  private AgentFactory(Constructor<A> constructor) {
    this.constructor = constructor;
  }

  /**
   * Reflects and returns a new {@link AgentFactory} for {@code clazz}.
   *
   * @param clazz to reflect.
   * @param <A>   the agent type.
   * @return a factory for constructing agents of type {@code A}.
   * @throws NoSuchMethodException if there is no zero-arg constructor in the agent.
   */
  public static <A extends Agent> AgentFactory<A> forClass(Class<A> clazz) throws NoSuchMethodException {
    try {
      Constructor<A> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      return new AgentFactory<>(constructor);
    } catch (NoSuchMethodException e) {
      throw new NoSuchMethodException("No zero-arg constructor in Agent");
    }
  }

  /**
   * Reflects agent {@code agent}. This involves reflecting the agent's lanes and setting their lane models with a
   * reference to the agent's {@link StateCollector}.
   */
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

  /**
   * Reflects a {@link Lane} and creates and sets its corresponding {@link LaneModel}. This allows for users to directly
   * operate on a {@link Lane} when a lifecycle event is fired (which happens on the {@link ai.swim.server.lanes.LaneView}
   * as well as the runtime setting its state using the {@link LaneModel}.
   */
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

  /**
   * Reflects and initialises a new {@link AgentModel}.
   *
   * @return an initialised agent.
   */
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
