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

package ai.swim.server.schema;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
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
  private final Map<Class<? extends AbstractAgent>, String> uriResolver;

  public PlaneSchema(Class<P> planeClass, String name, Map<String, AgentSchema<?>> agentSchemas,
      Map<Class<? extends AbstractAgent>, String> uriResolver) {
    this.planeClass = planeClass;
    this.name = name;
    this.agentSchemas = agentSchemas;
    this.uriResolver = uriResolver;
  }

  public static <P extends AbstractPlane> PlaneSchema<P> reflectSchema(Class<P> planeClass) throws SwimServerException {
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
    Map<Class<? extends AbstractAgent>, String> uriResolver = agentSchemas
        .entrySet()
        .stream()
        .collect(Collectors.toMap(entry -> entry.getValue().getAgentClass(), Map.Entry::getKey));

    return new PlaneSchema<>(planeClass, planeName, agentSchemas, uriResolver);
  }

  private static <P extends AbstractPlane> Map<String, AgentSchema<?>> reflectAgents(Class<P> planeClass) throws SwimServerException {
    Map<String, AgentSchema<?>> agentSchemas = new HashMap<>();
    Field[] fields = planeClass.getDeclaredFields();

    for (Field field : fields) {
      Class<?> fieldType = field.getType();
      SwimRoute routeAnno = field.getAnnotation(SwimRoute.class);

      if (routeAnno != null) {
        if (AbstractAgent.class.isAssignableFrom(fieldType)) {
          @SuppressWarnings("unchecked") Class<? extends AbstractAgent> agentClass = (Class<? extends AbstractAgent>) fieldType;
          String nodeUri = routeAnno.value();
          if (agentSchemas.containsKey(nodeUri)) {
            throw new SwimServerException("Duplicate node URI: " + nodeUri);
          }

          agentSchemas.put(nodeUri, AgentSchema.reflectSchema(agentClass));
        } else {
          throw new IllegalArgumentException(String.format(
              "%s is annotated with %s but its field does not extend from %s",
              field.getName(),
              SwimRoute.class.getCanonicalName(),
              AbstractAgent.class.getCanonicalName()));
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

  public AgentSchema<?> schemaFor(Class<? extends AbstractAgent> agentClass) {
    String uri = uriResolver.get(agentClass);
    return agentSchemas.get(uri);
  }

  public AgentSchema<?> schemaFor(String uri) {
    return agentSchemas.get(uri);
  }

  public int laneIdFor(Class<? extends AbstractAgent> agentClass, String nodeUri) {
    AgentSchema<?> schema = schemaFor(agentClass);
    return schema.getLaneSchemas().get(nodeUri).getLaneId();
  }

  public Class<P> getPlaneClass() {
    return planeClass;
  }
}
