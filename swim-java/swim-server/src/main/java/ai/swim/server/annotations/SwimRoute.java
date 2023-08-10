package ai.swim.server.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for defining a {@link ai.swim.server.agent.Agent} in a {@link ai.swim.server.plane.AbstractPlane}.
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SwimRoute {
  /**
   * The URI of the node.
   */
  String value();
}
