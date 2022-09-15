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

package ai.swim.structure.writer.std;

import ai.swim.structure.value.Item;
import ai.swim.structure.value.Value;
import ai.swim.structure.writer.value.ValueStructuralWriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStructuralWritableTest {

  @Test
  void writeMap() {
    Map<String,Integer> map = new TreeMap<>();
    map.put("a",1);
    map.put("b", 2);
    map.put("c", 3);
    map.put("d", 4);
    map.put("e", 5);

    MapStructuralWritable<String, Integer> mapWritable = new MapStructuralWritable<>(ScalarWriters.STRING, ScalarWriters.INTEGER);

    Value value = mapWritable.writeInto(map, new ValueStructuralWriter());
    assertTrue(value.isRecord());
    assertEquals(
        Value.ofItems(
            List.of(
                Item.of(Value.of("a"), Value.of(1)),
                Item.of(Value.of("b"), Value.of(2)),
                Item.of(Value.of("c"), Value.of(3)),
                Item.of(Value.of("d"), Value.of(4)),
                Item.of(Value.of("e"), Value.of(5))
            )
        ),
        value
    );
  }
}