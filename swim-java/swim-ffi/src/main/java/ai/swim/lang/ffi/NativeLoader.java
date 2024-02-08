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

package ai.swim.lang.ffi;

import java.io.IOException;

public class NativeLoader {
  private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

  /**
   * Attempts to load a native library from the classpath and falls back to attempting to load the correct library for the
   * OS in the JAR archive.
   *
   * @param name of the native library without a directory mapping.
   * @throws IOException if the native library could not be loaded.
   */
  public static void loadLibrary(String name) throws IOException {
    try {
      // try loading the library locally before inspecting the jar, in case we're running as part of a unit test/local
      // development instead of a released package.
      System.loadLibrary(name);
    } catch (Throwable ignored) {
      // fall back to attempting to load the library from the archive.

      if (isOs("linux")) {
        NativeUtils.loadLibraryFromJar(name + ".so");
      } else if (isOs("windows")) {
        NativeUtils.loadLibraryFromJar(name + ".dll");
      } else if (isOs("mac")) {
        NativeUtils.loadLibraryFromJar("/lib" + name + ".dylib");
      } else {
        throw new ExceptionInInitializerError("Unsupported OS: " + OS_NAME);
      }
    }
  }

  /**
   * Returns whether the current OS name is prefixed by `os`.
   */
  private static boolean isOs(String os) {
    return OS_NAME.startsWith(os);
  }

  public static void loadLibraries(String... libraries) throws IOException {
    if (libraries == null) {
      throw new NullPointerException();
    }

    for (String library : libraries) {
      NativeLoader.loadLibrary(library);
    }
  }
}
