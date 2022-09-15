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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionStructuralWritableTest {
  @Test
  void writeList() {
    List<Integer> list = List.of(1, 2, 3, 4, 5);
    ListStructuralWritable<Integer> listWritable = new ListStructuralWritable<>(ScalarWriters.INTEGER);

    Value value = listWritable.writeInto(list, new ValueStructuralWriter());
    assertTrue(value.isRecord());
    assertEquals(
        Value.ofItems(
            List.of(
                Item.valueItem(1),
                Item.valueItem(2),
                Item.valueItem(3),
                Item.valueItem(4),
                Item.valueItem(5)
            )
        ),
        value
    );
  }

}