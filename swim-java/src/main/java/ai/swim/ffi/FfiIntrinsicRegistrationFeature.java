package ai.swim.ffi;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import org.graalvm.nativeimage.hosted.Feature;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import java.lang.reflect.Method;
import java.util.Set;

@AutomaticFeature
public class FfiIntrinsicRegistrationFeature implements Feature {

  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    ConfigurationBuilder builder = new ConfigurationBuilder();
    Set<Scanner> scanners = builder
        .addScanners(Scanners.MethodsAnnotated)
        .forPackage("ai.swim")
        .getScanners();

    Reflections reflections = new Reflections(scanners);
    Set<Method> typesAnnotatedWith = reflections.getMethodsAnnotatedWith(FfiIntrinsic.class);

    for (Method target : typesAnnotatedWith) {
      Class<?> parent = target.getDeclaringClass();
      JNIRuntimeAccess.register(parent);
      JNIRuntimeAccess.register(target);
    }
 
    JNIRuntimeAccess.register(Object.class);

    try {
      JNIRuntimeAccess.register(Object.class.getDeclaredMethod("notify"));
      JNIRuntimeAccess.register(Object.class.getDeclaredMethod("wait"));
    } catch (NoSuchMethodException e) {
      throw new FfiInitialisationException(e);
    }
  }

}
