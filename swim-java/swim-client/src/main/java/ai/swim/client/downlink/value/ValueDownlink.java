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

package ai.swim.client.downlink.value;

import ai.swim.client.SwimClientException;
import ai.swim.concurrent.Trigger;

/**
 * A Swim Value Downlink representation. A ValueDownlink synchronizes a shared real-time value with a remote value lane
 * and provides lifecycle callbacks that may be registered to be notified of certain events.
 * <p>
 * This class is thread safe.
 *
 * @param <T> the type of the value.
 */
public abstract class ValueDownlink<T> extends Downlink<ValueDownlinkState<T>> {
  protected ValueDownlink(CountDownLatch stoppedBarrier, ValueDownlinkState<T> state) {
    super(stoppedBarrier, state);
  }
}
