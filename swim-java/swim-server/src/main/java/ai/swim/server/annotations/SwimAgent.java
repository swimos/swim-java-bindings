package ai.swim.server.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for defining a {@link ai.swim.server.agent.Agent}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwimAgent {
  /**
   * The name of the agent.
   * <p>
   * This is not the URI.
   */
  String value() default "";
}
