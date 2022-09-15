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

package ai.swim.structure.writer.proxy;

import ai.swim.structure.value.Value;
import ai.swim.structure.writer.ValueStructuralWritable;
import ai.swim.structure.writer.std.ListStructuralWritable;
import ai.swim.structure.writer.std.MapStructuralWritable;
import ai.swim.structure.writer.std.ScalarWriters;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriterProxyTest {

  @Test
  void resolvesPrimitives() {
    assertEquals(WriterProxy.getProxy().lookup(Integer.class), ScalarWriters.INTEGER);
    assertEquals(WriterProxy.getProxy().lookup(Integer.TYPE), ScalarWriters.INTEGER);
    assertEquals(WriterProxy.getProxy().lookup(String.class), ScalarWriters.STRING);
    assertEquals(WriterProxy.getProxy().lookup(Character.class), ScalarWriters.CHARACTER);
    assertEquals(WriterProxy.getProxy().lookup(Character.TYPE), ScalarWriters.CHARACTER);
    assertEquals(WriterProxy.getProxy().lookup(Long.class), ScalarWriters.LONG);
    assertEquals(WriterProxy.getProxy().lookup(Long.TYPE), ScalarWriters.LONG);
    assertEquals(WriterProxy.getProxy().lookup(Byte.class), ScalarWriters.BYTE);
    assertEquals(WriterProxy.getProxy().lookup(Byte[].class), ScalarWriters.BOXED_BLOB);
    assertEquals(WriterProxy.getProxy().lookup(Byte.TYPE), ScalarWriters.BYTE);
    assertEquals(WriterProxy.getProxy().lookup(Short.class), ScalarWriters.SHORT);
    assertEquals(WriterProxy.getProxy().lookup(Short.TYPE), ScalarWriters.SHORT);
    assertEquals(WriterProxy.getProxy().lookup(Boolean.class), ScalarWriters.BOOLEAN);
    assertEquals(WriterProxy.getProxy().lookup(Boolean.TYPE), ScalarWriters.BOOLEAN);
    assertEquals(WriterProxy.getProxy().lookup(Float.class), ScalarWriters.FLOAT);
    assertEquals(WriterProxy.getProxy().lookup(Float.TYPE), ScalarWriters.FLOAT);
    assertEquals(WriterProxy.getProxy().lookup(Double.class), ScalarWriters.DOUBLE);
    assertEquals(WriterProxy.getProxy().lookup(Double.TYPE), ScalarWriters.DOUBLE);
    assertEquals(WriterProxy.getProxy().lookup(byte[].class), ScalarWriters.PRIMITIVE_BLOB);
    assertEquals(WriterProxy.getProxy().lookup(BigInteger.class), ScalarWriters.BIG_INT);
    assertEquals(WriterProxy.getProxy().lookup(BigDecimal.class), ScalarWriters.BIG_DECIMAL);
    assertEquals(WriterProxy.getProxy().lookup(Value.class).getClass(), ValueStructuralWritable.class);
    assertEquals(WriterProxy.getProxy().lookup(AtomicInteger.class), ScalarWriters.ATOMIC_INTEGER);
    assertEquals(WriterProxy.getProxy().lookup(AtomicBoolean.class), ScalarWriters.ATOMIC_BOOLEAN);
    assertEquals(WriterProxy.getProxy().lookup(AtomicLong.class), ScalarWriters.ATOMIC_LONG);
  }

  @Test
  void throwsOnNull() {
    assertThrows(NullPointerException.class, () -> WriterProxy.getProxy().lookup(null));
  }

  @Test
  void resolvesStdTypes() {
    assertEquals(
        WriterProxy.getProxy().lookup(
            Map.class,
            WriterTypeParameter.forClass(String.class),
            WriterTypeParameter.forClass(Integer.class)
        ).getClass(),
        MapStructuralWritable.class
    );
    assertEquals(
        WriterProxy.getProxy().lookup(
            HashMap.class,
            WriterTypeParameter.forClass(String.class),
            WriterTypeParameter.forClass(Integer.class)
        ).getClass(),
        MapStructuralWritable.class
    );
    assertEquals(
        WriterProxy.getProxy().lookup(
            TreeMap.class,
            WriterTypeParameter.forClass(String.class),
            WriterTypeParameter.forClass(Integer.class)
        ).getClass(),
        MapStructuralWritable.class
    );

    assertEquals(
        WriterProxy.getProxy().lookup(
            List.class,
            WriterTypeParameter.forClass(String.class)
        ).getClass(),
        ListStructuralWritable.class
    );
    assertEquals(
        WriterProxy.getProxy().lookup(
            ArrayList.class,
            WriterTypeParameter.forClass(String.class)
        ).getClass(),
        ListStructuralWritable.class
    );
    assertEquals(
        WriterProxy.getProxy().lookup(
            ArrayDeque.class,
            WriterTypeParameter.forClass(String.class)
        ).getClass(),
        ListStructuralWritable.class
    );
  }

  @Test
  void typedMismatch() {
    assertThrows(IllegalArgumentException.class, () -> WriterProxy.getProxy().lookupTyped(Integer.class, WriterTypeParameter.forClass(Integer.class)));
    assertThrows(IllegalArgumentException.class, () -> WriterProxy.getProxy().lookupTyped(Map.class, WriterTypeParameter.forClass(Integer.class)));
    assertThrows(IllegalArgumentException.class, () -> WriterProxy.getProxy().lookupTyped(Map.class));
    assertThrows(IllegalArgumentException.class, () -> WriterProxy.getProxy().lookupTyped(
            Map.class,
            WriterTypeParameter.forClass(Integer.class),
            WriterTypeParameter.forClass(Integer.class),
            WriterTypeParameter.forClass(Integer.class)
        )
    );
  }

}