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

package ai.swim.structure.recognizer.bridge;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.proxy.RecognizerProxy;
import ai.swim.structure.value.Item;
import ai.swim.structure.value.Value;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("unchecked")
class RecognizerBridgeTest {
  private final RecognizerProxy proxy = RecognizerProxy.getProxy();

  @Test
  void transformNumeric() {
    assertEquals(proxy.lookup(Integer.TYPE).transform(Value.of(13)), 13);
    assertEquals(proxy.lookup(Long.TYPE).transform(Value.of(13L)), 13L);
    assertEquals(proxy.lookup(Float.TYPE).transform(Value.of(13f)), 13f);
    assertEquals(proxy.lookup(Double.TYPE).transform(Value.of(13d)), 13d);
    assertEquals(proxy.lookup(BigInteger.class).transform(Value.of(BigInteger.TEN)), BigInteger.TEN);
    assertEquals(proxy.lookup(BigDecimal.class).transform(Value.of(BigDecimal.TEN)), BigDecimal.TEN);
  }

  @Test
  void transformText() {
    assertEquals(proxy.lookup(String.class).transform(Value.of("Bonsoir, Elliot")), "Bonsoir, Elliot");
  }

  @Test
  void transformBlob() {
    assertArrayEquals(proxy.lookup(byte[].class).transform(Value.of(new byte[] {1, 2, 3})), new byte[] {1, 2, 3});
  }

  @Test
  void transformExtant() {
    assertNull(proxy.lookup(Void.class).transform(Value.extant()));
  }

  @Test
  void optFields() {
    Recognizer<Clazz<Integer, Integer>> rec = proxy.lookup((Class<Clazz<Integer, Integer>>) (Class<?>) Clazz.class);
    assertEquals(
        rec.transform(
            Value.of(
                List.of(Value.ofAttr("Clazz")),
                List.of(
                    Item.of(Value.of("first"), Value.of(2)),
                    Item.of(Value.of("second"), Value.of(3))
                       ))
                     ),
        new Clazz<>(2, 3)
                );

    rec = rec.reset();
    assertEquals(
        rec.transform(
            Value.of(
                List.of(Value.ofAttr("Clazz")),
                List.of(Item.of(Value.of("second"), Value.of(3))))
                     ),
        new Clazz<>(null, 3)
                );

    rec = rec.reset();
    assertEquals(
        rec.transform(
            Value.of(
                List.of(Value.ofAttr("Clazz")),
                List.of(
                    Item.of(Value.of("first"), Value.extant()),
                    Item.of(Value.of("second"), Value.of(3))
                       ))
                     ),
        new Clazz<>(null, 3)
                );

    rec = rec.reset();
    assertEquals(
        rec.transform(
            Value.of(
                List.of(Value.ofAttr("Clazz")),
                List.of(Item.of(Value.of("first"), Value.of(2))))
                     ),
        new Clazz<>(2, null)
                );

    rec = rec.reset();
    assertEquals(
        rec.transform(
            Value.of(
                List.of(Value.ofAttr("Clazz")),
                List.of(
                    Item.of(Value.of("first"), Value.of(2)),
                    Item.of(Value.of("second"), Value.extant())
                       ))
                     ),
        new Clazz<>(2, null)
                );

    rec = rec.reset();
    assertEquals(
        rec.transform(Value.ofAttrs(List.of(Value.ofAttr("Clazz")))),
        new Clazz<>(null, null)
                );

    rec = rec.reset();
    assertEquals(
        rec.transform(
            Value.of(
                List.of(Value.ofAttr("Clazz")),
                List.of(
                    Item.of(Value.of("first"), Value.extant()),
                    Item.of(Value.of("second"), Value.extant())
                       ))
                     ),
        new Clazz<>(null, null)
                );
  }

  @Test
  void nested() {
    Recognizer<Clazz<Integer, List<Integer>>> rec = proxy.lookup((Class<Clazz<Integer, List<Integer>>>) (Class<?>) Clazz.class);
    Clazz<Integer, List<Integer>> expected = new Clazz<>(13, List.of(1, 2, 3));

    assertEquals(
        expected,
        rec.transform(Value.of(
            List.of(Value.ofAttr("Clazz")),
            List.of(
                Value.ofItem(Value.of("first"), Value.of(13)),
                Value.ofItem(Value.of("second"), Value.ofItems(List.of(
                    Item.of(Value.of(1)),
                    Item.of(Value.of(2)),
                    Item.of(Value.of(3))
                                                                      )))
                   )
                              ))
                );
  }

  @AutoForm
  public static class Clazz<A, B> {
    public A first;
    public B second;

    public Clazz() {
    }

    public Clazz(A first, B second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Clazz<?, ?> clazz = (Clazz<?, ?>) o;
      return Objects.equals(first, clazz.first) && Objects.equals(second, clazz.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "Clazz{" +
          "first=" + first +
          ", second=" + second +
          '}';
    }
  }

}