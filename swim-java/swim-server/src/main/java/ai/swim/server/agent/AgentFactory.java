/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server.agent;

import ai.swim.server.annotations.SwimLane;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.map.MapLane;
import ai.swim.server.lanes.map.MapLaneView;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.schema.AgentSchema;
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
 * An initialised agent is an {@link AgentView}s that is used to dispatch events from the Rust runtime to lanes;
 * decoding the message requests, setting the state of the lanes, invoking user-defined lifecycle events and encoding
 * responses.
 *
 * @param <A>
 */
public class AgentFactory<A extends AbstractAgent> {
  private final String agentName;
  private final Constructor<A> constructor;
  private final Map<String, Integer> laneMappings;

  private AgentFactory(String agentName, Constructor<A> constructor, Map<String, Integer> laneMappings) {
    this.agentName = agentName;
    this.constructor = constructor;
    this.laneMappings = laneMappings;
  }

  /**
   * Reflects and returns a new {@link AgentFactory} for {@code agentSchema}.
   *
   * @param <A>         the agent type.
   * @param agentSchema to reflect.
   * @return a factory for constructing agents of type {@code A}.
   * @throws NoSuchMethodException if there is no zero-arg constructor in the agent.
   */
  public static <A extends AbstractAgent> AgentFactory<A> forSchema(AgentSchema<A> agentSchema) throws NoSuchMethodException {
    try {
      Class<A> agentClass = agentSchema.getAgentClass();
      Constructor<A> constructor = agentClass.getDeclaredConstructor(AgentContext.class);
      constructor.setAccessible(true);
      return new AgentFactory<>(agentSchema.getAgentName(), constructor, agentSchema.laneMappings());
    } catch (NoSuchMethodException e) {
      throw new NoSuchMethodException("Missing constructor with AgentContext");
    }
  }

  /**
   * Reflects agent {@code agent}. This involves reflecting the agent's lanes and setting their lane models with a
   * reference to the agent's {@link StateCollector}.
   */
  private static <A extends AbstractAgent> AgentView reflectAgent(A agent, Map<String, Integer> laneMappings) {
    Class<? extends AbstractAgent> agentClass = agent.getClass();
    Field[] fields = agentClass.getDeclaredFields();
    Map<Integer, LaneModel> lanes = new HashMap<>();
    StateCollector collector = new StateCollector();

    for (Field field : fields) {
      if (Lane.class.isAssignableFrom(field.getType())) {
        SwimLane anno = field.getAnnotation(SwimLane.class);
        if (anno != null) {
          String annoValue = anno.value();
          annoValue = annoValue.isEmpty() ? null : annoValue;
          String laneUri = Objects.requireNonNullElse(annoValue, field.getName());

          Type type = field.getGenericType();

          if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            Integer laneId = laneMappings.get(laneUri);

            field.setAccessible(true);

            if (rawType instanceof Class<?>) {
              lanes.put(laneId, reflectLane(agent, laneUri, laneId, field, (Class<?>) rawType, collector));
            } else {
              throw unsupportedLaneType(type, agentClass);
            }
          } else {
            throw unsupportedLaneType(type, agentClass);
          }
        }
      }
    }

    return new AgentView(agent, new AgentNode(collector, lanes, laneMappings));
  }

  private static <A extends AbstractAgent> IllegalArgumentException unsupportedLaneType(Type type,
      Class<A> agentClass) {
    return new IllegalArgumentException("Unsupported lane type: " + type + " in " + agentClass.getCanonicalName());
  }

  /**
   * Reflects a {@link Lane} and creates and sets its corresponding {@link LaneModel}. This allows for users to directly
   * operate on a {@link Lane} when a lifecycle event is fired (which happens on the {@link ai.swim.server.lanes.LaneView}
   * as well as the runtime setting its state using the {@link LaneModel}.
   */
  private static LaneModel reflectLane(AbstractAgent agent,
      String laneUri,
      int laneId,
      Field field,
      Class<?> type,
      StateCollector collector) {
    if (ValueLane.class.isAssignableFrom(type)) {
      return reflectValueLane(agent, laneUri, laneId, field, collector);
    } else if (MapLane.class.isAssignableFrom(type)) {
      return reflectMapLane(agent, laneUri, laneId, field, collector);
    } else {
      throw unsupportedLaneType(type, agent.getClass());
    }
  }

  private static LaneModel reflectValueLane(AbstractAgent agent,
      String laneUri,
      int laneId,
      Field field,
      StateCollector collector) {
    try {
      ValueLaneView<?> laneView = (ValueLaneView<?>) field.get(agent);
      return laneView.initLaneModel(collector, laneId);
    } catch (IllegalAccessException e) {
      throw laneInitFailure(agent, laneUri, e);
    }
  }

  private static LaneModel reflectMapLane(AbstractAgent agent,
      String laneUri,
      int laneId,
      Field field,
      StateCollector collector) {
    try {
      MapLaneView<?, ?> laneView = (MapLaneView<?, ?>) field.get(agent);
      return laneView.initLaneModel(collector, laneId);
    } catch (IllegalAccessException e) {
      throw laneInitFailure(agent, laneUri, e);
    }
  }

  private static AgentInitializationException laneInitFailure(AbstractAgent agent,
      String laneUri,
      IllegalAccessException e) {
    return new AgentInitializationException(String.format(
        "Failed to initialise lane '%s' on agent '%s'",
        laneUri,
        agent.getContext().getAgentName()), e);
  }

  /**
   * Reflects and initialises a new {@link AgentView}.
   *
   * @return an initialised agent.
   */
  public AgentView newInstance(long agentContextPtr) {
    try {
      AgentContext context = new AgentContext(agentContextPtr, agentName);
      constructor.setAccessible(true);
      A agent = constructor.newInstance(context);

      AgentView agentView = reflectAgent(agent, laneMappings);
      context.setAgent(agentView.getNode());

      return agentView;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new AgentInitializationException("Failed to create new agent", e);
    }
  }

  public int idFor(String laneUri) {
    Integer id = this.laneMappings.get(laneUri);
    if (id == null) {
      throw new IllegalArgumentException("Unregistered lane: " + laneUri);
    } else {
      return id;
    }
  }

  @Override
  public String toString() {
    return "AgentFactory{" +
        "agentName='" + agentName + '\'' +
        ", constructor=" + constructor +
        ", laneMappings=" + laneMappings +
        '}';
  }
}
