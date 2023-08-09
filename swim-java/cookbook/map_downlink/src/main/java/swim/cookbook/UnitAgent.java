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

import swim.api.SwimLane;
import swim.api.agent.AbstractAgent;
import swim.api.lane.MapLane;
import swim.concurrent.TimerRef;

import java.util.Random;

/**
 * A simple Swim agent that contains a value lane that contains an integer.
 */
public class UnitAgent extends AbstractAgent {

  /**
   * A simple map lane with integer keys and string values.
   */
  @SwimLane("lane")
  MapLane<Integer, String> mapLane = this.<Integer,String>mapLane()
      // Register a lifecycle event that is invoked when map sets a new key-value pair.
      .didUpdate((key, newValue, oldValue) -> {
        System.out.printf("Set key '%s' from '%s' to '%s'\n", key, oldValue, newValue);
      });

  private TimerRef timer;

  /**
   * Registers a timer that is fired every second which puts a key-value pair into the map lane.
   */
  @Override
  public void didStart() {
    Random random = new Random();
    this.timer = setTimer(1000, () -> {
      int key = random.nextInt(10000);
      int value = random.nextInt(10000);
      this.mapLane.put(key, Integer.toString(value));
      this.timer.reschedule(1000);
    });
  }

}
