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

package ai.swim.structure.write.header;

import ai.swim.structure.write.BodyWriter;
import ai.swim.structure.write.StructuralWriter;
import ai.swim.structure.write.Writable;
import ai.swim.structure.write.std.ScalarWriters;

import java.util.Objects;

public class Header {

  public interface AppendHeaders {
    int numItems();

    <T, B extends BodyWriter<T>> B append(B writer);
  }

  public static class NoSlots implements AppendHeaders {
    public static <V> HeaderSlots<V> prepend(String key, V value) {
      return new HeaderSlots<>(key, null, value, new NoSlots());
    }

    public static <V> HeaderSlots<V> prepend(String key, Writable<V> valueWriter, V value) {
      Objects.requireNonNull(valueWriter);
      return new HeaderSlots<>(key, valueWriter, value, new NoSlots());
    }

    @Override
    public int numItems() {
      return 0;
    }

    @Override
    public <T, B extends BodyWriter<T>> B append(B writer) {
      return writer;
    }
  }

  public static class HeaderSlots<V> implements AppendHeaders {
    private final String key;
    private final V value;
    private final AppendHeaders tail;
    private final Writable<V> valueWriter;

    public HeaderSlots(String key, Writable<V> valueWriter, V value, AppendHeaders tail) {
      this.key = key;
      this.valueWriter = valueWriter;
      this.value = value;
      this.tail = tail;
    }

    @Override
    public int numItems() {
      return tail.numItems() + (value == null ? 0 : 1);
    }

    public <N> HeaderSlots<N> prepend(String key, N value) {
      return new HeaderSlots<>(key, null, value, this);
    }

    public <N> HeaderSlots<N> prepend(String key, Writable<N> nWritable, N value) {
      return new HeaderSlots<>(key, nWritable, value, this);
    }

    @Override
    public <T, B extends BodyWriter<T>> B append(B writer) {
      if (value == null) {
        return writer;
      }

      if (valueWriter != null) {
        writer.writeSlot(ScalarWriters.STRING, key, valueWriter, value);
      } else {
        writer.writeSlot(key, value);
      }

      return tail.append(writer);
    }

    public <N> HeaderWithBody<N> withBody(N value) {
      return new HeaderWithBody<>(null, value, this);
    }

    public <N> HeaderWithBody<N> withBody(Writable<N> valueWriter, N value) {
      Objects.requireNonNull(valueWriter);
      return new HeaderWithBody<>(valueWriter, value, this);
    }

    public SimpleHeader<V> simple() {
      return new SimpleHeader<>(this);
    }
  }

  public static class SimpleHeader<V> implements WritableHeader {
    private final HeaderSlots<V> headerSlots;

    public SimpleHeader(HeaderSlots<V> headerSlots) {
      this.headerSlots = headerSlots;
    }

    @Override
    public <T> T writeInto(StructuralWriter<T> structuralWriter) {
      BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(headerSlots.numItems());
      return headerSlots.append(bodyWriter).done();
    }
  }

  public static class HeaderWithBody<V> implements WritableHeader {
    private final AppendHeaders slots;
    private final V value;
    private final Writable<V> valueWriter;

    HeaderWithBody(Writable<V> valueWriter, V value, AppendHeaders slots) {
      this.valueWriter = valueWriter;
      this.value = value;
      this.slots = slots;
    }

    public int len() {
      return slots.numItems() + 1;
    }

    @Override
    public <T> T writeInto(StructuralWriter<T> structuralWriter) {
      BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len());

      if (valueWriter != null) {
        bodyWriter.writeValue(valueWriter, value);
      } else {
        bodyWriter.writeValue(value);
      }

      slots.append(bodyWriter);
      return bodyWriter.done();
    }
  }

}
