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

package ai.swim.server.agent.call;

/**
 * Exception thrown when a thread attempts to illegally access the Swim runtime.
 */
public class CallContextException extends RuntimeException {
  public CallContextException(String message) {
    super(message);
  }

  public static CallContextException illegalAccess(CallContext.ThreadInfo state) {
    String msg = String.format(
        "Attempted to access Swim runtime outside of context; all events must be fired from the Swim runtime. Thread: '%s' is not registered for access",
        state);
    return new CallContextException(msg);
  }
}
