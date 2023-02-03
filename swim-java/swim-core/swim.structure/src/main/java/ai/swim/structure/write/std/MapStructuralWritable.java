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

package ai.swim.structure.write.std;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.write.BodyWriter;
import ai.swim.structure.write.StructuralWritable;
import ai.swim.structure.write.StructuralWriter;
import ai.swim.structure.write.Writable;
import ai.swim.structure.write.proxy.WriterProxy;
import ai.swim.structure.write.proxy.WriterTypeParameter;

import java.util.Map;

public class MapStructuralWritable<K, V> implements StructuralWritable<Map<K, V>> {
  private Writable<K> kWriter;
  private Writable<V> vWriter;
  private Class<K> kClass;
  private Class<V> vClass;

  @AutoForm.TypedConstructor
  public MapStructuralWritable(WriterTypeParameter<K> kWriter, WriterTypeParameter<V> vWriter) {
    this.kWriter = kWriter.build();
    this.vWriter = vWriter.build();
  }

  public MapStructuralWritable() {

  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T writeInto(Map<K, V> from, StructuralWriter<T> structuralWriter) {
    int len = from.size();
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (Map.Entry<K, V> entry : from.entrySet()) {
      K key = entry.getKey();
      V value = entry.getValue();

      if (key != null && key.getClass() != kClass) {
        this.kWriter = WriterProxy.getProxy().lookupObject(key);
        this.kClass = (Class<K>) key.getClass();
      }

      if (value != null && value.getClass() != vClass) {
        this.vWriter = WriterProxy.getProxy().lookupObject(value);
        this.vClass = (Class<V>) value.getClass();
      }

      bodyWriter = bodyWriter.writeSlot(kWriter, key, vWriter, value);
    }

    return bodyWriter.done();
  }
}
