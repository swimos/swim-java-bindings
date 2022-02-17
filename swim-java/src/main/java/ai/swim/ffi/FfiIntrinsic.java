package ai.swim.ffi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to mark objects as only accessed through the JNI.
 * <p>
 * Any constructor or method marked with this annotation will automatically be registered during Graal's native image
 * build analysis phase so they are accessible via the JNI at image runtime. Anything not annotated will not be
 * accessible through the JNI and will cause a segmentation fault.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface FfiIntrinsic {

}
