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
import swim.api.lane.ValueLane;
import swim.concurrent.TimerRef;

/**
 * A simple Swim agent that contains a value lane that contains an integer.
 */
public class UnitAgent extends AbstractAgent {

  /**
   * A simple value lane that stores an integer.
   */
  @SwimLane("lane")
  ValueLane<Integer> intLane = this.<Integer>valueLane()
      // Register a lifecycle event that is invoked when lane sets a new value.
      .didSet((newValue, oldValue) -> {
        System.out.println("Set: " + newValue + ", " + oldValue);
      });

  private TimerRef timer;

  /**
   * Registers a timer that is fired every second which increments the lane's value.
   */
  @Override
  public void didStart() {
//    this.timer = setTimer(1000, () -> {
//      this.intLane.set(this.intLane.get() + 1);
//      this.timer.reschedule(1000);
//    });
  }

}
