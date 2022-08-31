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

package ai.swim.structure.writer;

import java.util.Objects;

public interface BodyWriter<T> {
  <K, V> BodyWriter<T> writeSlotWith(StructuralWritable<K> keyWriter, K key, StructuralWritable<V> valueWriter, V value);

  default  <K, V> BodyWriter<T> writeSlot(K key, V value) {
    StructuralWritable<K> keyWriter = WriterProxy.lookup(Objects.requireNonNull(key).getClass());
    StructuralWritable<V> valueWriter = WriterProxy.lookup(Objects.requireNonNull(value).getClass());
    return this.writeSlotWith(keyWriter, key, valueWriter, value);
  }

  <V> BodyWriter<T> writeValueWith(StructuralWritable<V> writer, V value);

  default <V> BodyWriter<T> writeValue(StructuralWritable<V> writer, V value) {
    StructuralWritable<V> valueWriter = WriterProxy.lookup(Objects.requireNonNull(value).getClass());
    return this.writeValue(valueWriter, value);
  }

  T done();
}
