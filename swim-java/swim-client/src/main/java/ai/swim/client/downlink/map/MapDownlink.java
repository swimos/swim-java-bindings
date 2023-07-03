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

package ai.swim.client.downlink.map;

import ai.swim.client.downlink.Downlink;

import java.util.concurrent.CountDownLatch;

/**
 * A Swim Map Downlink representation. A MapDownlink synchronizes a shared real-time value with a remote map lane
 * and provides lifecycle callbacks that may be registered to be notified of certain events.
 * <p>
 * This class is thread safe.
 *
 * @param <K> the type of the map's key.
 * @param <V> the type of the map's key.
 */
public abstract class MapDownlink<K, V> extends Downlink<MapDownlinkState<K, V>> {
  protected MapDownlink(CountDownLatch stoppedBarrier, MapDownlinkState<K, V> state) {
    super(stoppedBarrier, state);
  }
}
