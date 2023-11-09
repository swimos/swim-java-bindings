package ai.swim.server.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for defining a {@link ai.swim.server.agent.AbstractAgent}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwimAgent {
  /**
   * The name of the agent. If one is not defined here, then the agent's name will be what is returned from {@link Class#getSimpleName()}.
   * <p>
   * This is not the URI.
   */
  String value() default "";
}
