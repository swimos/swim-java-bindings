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
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.proxy.WriterProxy;
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

  @Test
  void writeArray() {
    Integer[] array = new Integer[] {1, 2, 3, 4, 5};
    Writable<Integer[]> writable = WriterProxy.getProxy().arrayType(Integer.TYPE);

    Value value = writable.writeInto(array, new ValueStructuralWriter());
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

  @Test
  void arrayResolvesWriter() {
//    Integer[] array = new Integer[]{1,2,3,4,5};
//    Writable<>

    List<int[]> list = List.of(new int[]{1});
    Writable<List<int[]>> writer = new ListStructuralWritable<>();

    Value actual = writer.writeInto(list, new ValueStructuralWriter());
  }

  @Test
  void listResolvesWriter() {
    List<Integer> list = List.of(1, 2, 3);
    Writable<List<Integer>> writer = new ListStructuralWritable<>();

    Value actual = writer.writeInto(list, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(
        Item.valueItem(1),
        Item.valueItem(2),
        Item.valueItem(3)
    ));

    assertEquals(expected, actual);
  }

  @Test
  void nestedListResolvesWriter() {
    List<List<Integer>> list = List.of(
        List.of(1,2),
        List.of(3,4),
        List.of(5,6)
    );
    Writable<List<List<Integer>>> writer = new ListStructuralWritable<>();

    Value actual = writer.writeInto(list,new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(
        Item.of(Value.ofItems(List.of(Item.valueItem(1), Item.valueItem(2)))),
        Item.of(Value.ofItems(List.of(Item.valueItem(3), Item.valueItem(4)))),
        Item.of(Value.ofItems(List.of(Item.valueItem(5), Item.valueItem(6))))
    ));

    assertEquals(expected, actual);
  }

}