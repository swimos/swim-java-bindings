// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.server;

/**
 * Exception thrown when a component (plane, agent, lane) used for building a Swim Server is not well-defined.
 */
public class SwimServerException extends Exception {
  public SwimServerException() {
  }

  public SwimServerException(String message) {
    super(message);
  }

  public SwimServerException(String message, Throwable cause) {
    super(message, cause);
  }

  public SwimServerException(Throwable cause) {
    super(cause);
  }
}
