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

package ai.swim.client.downlink;

import ai.swim.client.SwimClientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigTest extends FfiTest {

  private static native void parsesConfig(byte[] downlinkConfig);

  @Test
  void parsesValidConfig() {
    byte[] downlinkConfig = new DownlinkConfig().toArray();
    parsesConfig(downlinkConfig);
  }

  @Test
  void throwsOnInvalidConfig() {
    assertThrows(SwimClientException.class, () -> {
      byte[] downlinkConfig = new byte[] {1, 2, 3, 4, 5};
      parsesConfig(downlinkConfig);
    }, "Failed to parse downlink configuration: \"Invalid buffer length\"");
  }

}
