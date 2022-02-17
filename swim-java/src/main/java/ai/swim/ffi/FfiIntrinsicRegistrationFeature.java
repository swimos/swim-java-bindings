// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
