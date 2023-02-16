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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapDownlinkStateTest {

  @Test
  void drop() {
    HashMap<String, String> init = new HashMap<>(Map.of(
        "a", "a", "b", "b", "c", "c"
    ));
    HashMap<String, String> removed = new HashMap<>();
    MapDownlinkState<String, String> state = new MapDownlinkState<>(null, null, (key, map, value) -> {
      removed.put(key, value);
    }, init);

    state.drop().accept(2,true);
    assertEquals(2, removed.size());

    state.wrapOnClear(removed::putAll).accept(true);
    assertEquals(init, removed);
  }

  @Test
  void take() {
    HashMap<String, String> init = new HashMap<>(Map.of(
        "a", "a", "b", "b", "c", "c", "d", "d", "e", "e"
    ));
    HashMap<String, String> removed = new HashMap<>();
    MapDownlinkState<String, String> state = new MapDownlinkState<>(null, null, (key, map, value) -> {
      removed.put(key, value);
    }, init);

    state.take().accept(2,true);
    assertEquals(3, removed.size());

    state.wrapOnClear(removed::putAll).accept(true);
    assertEquals(init, removed);
  }

}