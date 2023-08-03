// Copyright 2015-2022 SWIM.AI inc.
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

package swim.cookbook;

import ai.swim.client.SwimClient;
import ai.swim.client.SwimClientException;
import ai.swim.client.downlink.value.ValueDownlink;
import ai.swim.client.downlink.value.ValueDownlinkBuilder;
import java.io.IOException;

/**
 * An example application that demonstrates interacting with a Swim Value Lane.
 */
final class CustomClient {

  private CustomClient() {

  }

  public static void main(String[] args) throws SwimClientException, IOException {
    // Initialises the client runtime.
    try (SwimClient client = SwimClient.open()) {

      // Here we create a ValueDownlinkBuilder to a host running at "warp://localhost:9001/" an agent of "unit/foo" and a
      // value lane of "lane" - which corresponds to UnitAgent#intLane.
      ValueDownlinkBuilder<Integer> builder = client.valueDownlink(
          "warp://localhost:9001/",
          "unit/foo",
          "lane",
          Integer.class);

      // Using this builder, we can register callbacks that will be invoked for the various lifecycle events that occur
      // when running the downlink.
      //
      // Only one callback may be registered for each lifecycle event type; repeated calls to set a callback will replace
      // the previous callback.
      ValueDownlink<Integer> downlink = builder
          // Registers a callback that is invoked when the downlink links to the host.
          .setOnLinked(() -> System.out.println("Downlink linked"))
          // Registers a callback that is invoked when the downlink synchronises its state with the lane.
          .setOnSynced((value) -> System.out.println("Downlink synced with: " + value))
          // Registers a callback that is invoked when the downlink receives an event.
          .setOnEvent((value -> System.out.println("Downlink received an event: " + value)))
          // Registers a callback that is invoked when the downlink sets a value.
          .setOnSet((oldValue, newValue) -> System.out.printf("Downlink value set from %s to %s\n", oldValue, newValue))
          // Registers a callback that is invoked when the downlink unlinks to the host.
          .setOnUnlinked(() -> System.out.println("Downlink unlinked"))
          // Open the downlink.
          .open();

      // Await the downlink stopping. The downlink will stop when it encounters an error or the peer closes.
      downlink.awaitStopped();
    }
  }

}
