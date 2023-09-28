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

/**
 * A schema for a {@link AbstractPlane}. Contains all of the {@link AgentSchema}s defined in the plane and provides
 * functionality for producing a msgpack representation of the plane.
 *
 * @param <P> the type of the plane
 */
public class PlaneSchema<P extends AbstractPlane> {
  /**
   * The {@link Class} implementor of {@link AbstractPlane}
   */
  private final Class<P> planeClass;
  /**
   * The name of the plane
   */
  private final String name;
  /**
   * Mapping from node URI to its {@link AgentSchema}
   */
  private final Map<String, AgentSchema<?>> agentSchemas;

  public PlaneSchema(Class<P> planeClass, String name, Map<String, AgentSchema<?>> agentSchemas) {
    this.planeClass = planeClass;
    this.name = name;
    this.agentSchemas = agentSchemas;
  }

  /**
   * Reflects a {@link PlaneSchema} from a {@link Class}.
   *
   * @param planeClass to reflect
   * @param <P>        the type of the plane
   * @return a {@link PlaneSchema} representing the provided class
   * @throws SwimServerException if the class is not well-defined
   */
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

    return new PlaneSchema<>(planeClass, planeName, agentSchemas);
  }

  /**
   * Builds a mapping from nodeUri -> {@link AgentSchema}.
   *
   * @param planeClass to reflect
   * @param <P>        the type of the plane
   * @return a mapping from nodeUri -> {@link AgentSchema}.
   * @throws SwimServerException if the plane is not well-defined.
   */
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
    return "PlaneSchema{" + "planeClass=" + planeClass + ", name='" + name + '\'' + ", agentSchemas=" + agentSchemas + '}';
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
        that.agentSchemas);
  }

  @Override
  public int hashCode() {
    return Objects.hash(planeClass, name, agentSchemas);
  }

  /**
   * Serializes this {@link PlaneSchema} into its msgpack representation
   *
   * @return this {@link PlaneSchema} into its msgpack representation
   * @throws IOException when the underlying IO in the {@link MessageBufferPacker} throws an exception
   */
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

  /**
   * Returns a schema for the nodeUri if it exists
   *
   * @param nodeUri to resolve
   * @return the schema for the nodeUri if it exists
   */
  public AgentSchema<?> schemaFor(String nodeUri) {
    return agentSchemas.get(nodeUri);
  }

  /**
   * Returns the class that this {@link PlaneSchema} represents
   *
   * @return the class that this {@link PlaneSchema} represents
   */
  public Class<P> getPlaneClass() {
    return planeClass;
  }
}
