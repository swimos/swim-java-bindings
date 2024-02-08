/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    ListStructuralWritable<Integer> listWritable = new ListStructuralWritable<>();

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

  <A> void arrayTest(A[] array, Class<A[]> arrayClass, List<Item> items) {
    Writable<A[]> writable = WriterProxy.getProxy().lookup(arrayClass);
    Value actual = writable.writeInto(array, new ValueStructuralWriter());
    Value expected = Value.ofItems(items);
    assertEquals(actual, expected);
  }

  @Test
  void intArrayTypes() {
    Writable<int[]> writable = WriterProxy.getProxy().lookup(int[].class);
    Value actual = writable.writeInto(new int[] {1, 2, 3}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(Item.valueItem(1), Item.valueItem(2), Item.valueItem(3)));
    assertEquals(actual, expected);

    arrayTest(
        new Integer[] {1, 2, 3},
        Integer[].class,
        List.of(Item.valueItem(1), Item.valueItem(2), Item.valueItem(3)));
  }

  @Test
  void charArrayTypes() {
    Writable<char[]> writable = WriterProxy.getProxy().lookup(char[].class);
    Value actual = writable.writeInto(new char[] {'a', 'b', 'c'}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(Item.valueItem('a'), Item.valueItem('b'), Item.valueItem('c')));
    assertEquals(actual, expected);

    arrayTest(
        new Character[] {'a', 'b', 'c'},
        Character[].class,
        List.of(Item.valueItem('a'), Item.valueItem('b'), Item.valueItem('c')));
  }

  @Test
  void doubleArrayTypes() {
    Writable<double[]> writable = WriterProxy.getProxy().lookup(double[].class);
    Value actual = writable.writeInto(new double[] {1.0, 2.0, 3.0}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(Item.valueItem(1.0), Item.valueItem(2.0), Item.valueItem(3.0)));
    assertEquals(actual, expected);

    arrayTest(
        new Double[] {1.0, 2.0, 3.0},
        Double[].class,
        List.of(Item.valueItem(1.0), Item.valueItem(2.0), Item.valueItem(3.0)));
  }

  @Test
  void longArrayTypes() {
    Writable<long[]> writable = WriterProxy.getProxy().lookup(long[].class);
    Value actual = writable.writeInto(new long[] {1L, 2L, 3L}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(Item.valueItem(1L), Item.valueItem(2L), Item.valueItem(3L)));
    assertEquals(actual, expected);

    arrayTest(
        new Long[] {1L, 2L, 3L},
        Long[].class,
        List.of(Item.valueItem(1L), Item.valueItem(2L), Item.valueItem(3L)));
  }

  @Test
  void shortArrayTypes() {
    Writable<short[]> writable = WriterProxy.getProxy().lookup(short[].class);
    Value actual = writable.writeInto(new short[] {(short) 1, (short) 2, (short) 3}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(
        Item.valueItem((short) 1),
        Item.valueItem((short) 2),
        Item.valueItem((short) 3)));
    assertEquals(actual, expected);

    arrayTest(
        new Short[] {1, 2, 3},
        Short[].class,
        List.of(Item.valueItem((short) 1), Item.valueItem((short) 2), Item.valueItem((short) 3)));
  }

  @Test
  void booleanArrayTypes() {
    Writable<boolean[]> writable = WriterProxy.getProxy().lookup(boolean[].class);
    Value actual = writable.writeInto(new boolean[] {true, false, true}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(Item.valueItem(true), Item.valueItem(false), Item.valueItem(true)));
    assertEquals(actual, expected);

    arrayTest(
        new Boolean[] {true, false, true},
        Boolean[].class,
        List.of(Item.valueItem(true), Item.valueItem(false), Item.valueItem(true)));
  }

  @Test
  void floatArrayTypes() {
    Writable<float[]> writable = WriterProxy.getProxy().lookup(float[].class);
    Value actual = writable.writeInto(new float[] {1.0f, 2.0f, 3.0f}, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(Item.valueItem(1.0f), Item.valueItem(2.0f), Item.valueItem(3.0f)));
    assertEquals(actual, expected);

    arrayTest(
        new Float[] {1.0f, 2.0f, 3.0f},
        Float[].class,
        List.of(Item.valueItem(1.0f), Item.valueItem(2.0f), Item.valueItem(3.0f)));
  }

  @Test
  void arrayResolvesWriter() {
    List<int[]> list = List.of(new int[] {1}, new int[] {2}, new int[] {3});
    Writable<List<int[]>> writer = new ListStructuralWritable<>();

    Value actual = writer.writeInto(list, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(
        Item.of(Value.ofItems(List.of(Item.valueItem(1)))),
        Item.of(Value.ofItems(List.of(Item.valueItem(2)))),
        Item.of(Value.ofItems(List.of(Item.valueItem(3))))
                                          ));

    assertEquals(expected, actual);
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
        List.of(1, 2),
        List.of(3, 4),
        List.of(5, 6)
                                      );
    Writable<List<List<Integer>>> writer = new ListStructuralWritable<>();

    Value actual = writer.writeInto(list, new ValueStructuralWriter());
    Value expected = Value.ofItems(List.of(
        Item.of(Value.ofItems(List.of(Item.valueItem(1), Item.valueItem(2)))),
        Item.of(Value.ofItems(List.of(Item.valueItem(3), Item.valueItem(4)))),
        Item.of(Value.ofItems(List.of(Item.valueItem(5), Item.valueItem(6))))
                                          ));

    assertEquals(expected, actual);
  }

}