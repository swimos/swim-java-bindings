package ai.swim.server.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for defining a {@link ai.swim.server.lanes.Lane}. Lanes that are decorated with this annotation
 * will have their fields automatically initialized; similar to Spring's {@code AutoWired} annotation and Java's {@code @Inject}
 * annotation.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SwimLane {
  /**
   * The URI of the lane.
   */
  String value() default "";
}
