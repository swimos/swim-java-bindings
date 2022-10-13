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

import ai.swim.structure.TypeParameter;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.writer.BodyWriter;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.proxy.WriterProxy;

import java.util.Map;

public class MapStructuralWritable<K, V> implements StructuralWritable<Map<K, V>> {
  private Writable<K> kWriter;
  private Writable<V> vWriter;

  public MapStructuralWritable(Writable<K> kWriter, Writable<V> vWriter) {
    this.kWriter = kWriter;
    this.vWriter = vWriter;
  }

  @AutoForm.TypedConstructor
  public MapStructuralWritable(TypeParameter<Writable<K>> kWriter, TypeParameter<Writable<V>> vWriter) {
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

    if (len != 0) {
      Map.Entry<K, V> entry = from.entrySet().iterator().next();
      if (kWriter == null) {
        kWriter = (Writable<K>) WriterProxy.getProxy().lookup(entry.getKey().getClass());
      }
      if (vWriter == null && entry.getValue() != null) {
        vWriter = (Writable<V>) WriterProxy.getProxy().lookup(entry.getValue().getClass());
      }
    }

    for (Map.Entry<K, V> entry : from.entrySet()) {
      bodyWriter = bodyWriter.writeSlot(kWriter, entry.getKey(), vWriter, entry.getValue());
    }

    return bodyWriter.done();
  }
}
