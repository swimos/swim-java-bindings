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

public class ExceptionUtils {
  public static String formatThrowable(Throwable exception) {
    if (exception == null) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    builder.append(exception);
    builder.append(System.lineSeparator());

    StackTraceElement[] stackTrace = exception.getStackTrace();
    if (stackTrace == null) {
      return builder.toString();
    }

    int lim = Math.min(stackTrace.length, 20);

    for (int i = 0; i < lim; i++) {
      StackTraceElement element = stackTrace[i];
      if (element == null) {
        break;
      }
      builder.append(element);
      builder.append(System.lineSeparator());
    }

    return builder.toString();
  }

  public static void flushOutputStreams() {
    System.out.flush();
    System.err.flush();
  }

}
