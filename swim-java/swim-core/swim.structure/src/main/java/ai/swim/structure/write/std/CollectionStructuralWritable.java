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

import ai.swim.structure.write.BodyWriter;
import ai.swim.structure.write.StructuralWritable;
import ai.swim.structure.write.StructuralWriter;
import ai.swim.structure.write.Writable;
import ai.swim.structure.write.proxy.WriterProxy;

import java.util.Collection;

public abstract class CollectionStructuralWritable<E, C extends Collection<E>> implements StructuralWritable<C> {
  private Class<E> eClass;
  private Writable<E> eWritable;

  public CollectionStructuralWritable(Writable<E> eWritable, Class<E> eClass) {
    this.eWritable = eWritable;
    this.eClass = eClass;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T writeInto(C from, StructuralWriter<T> structuralWriter) {
    int len = from.size();
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    if (len != 0 && eWritable == null) {
      E first = from.iterator().next();
      eWritable = WriterProxy.getProxy().lookupObject(first);
    }

    for (E e : from) {
      if (e != null && e.getClass() != eClass) {
        eWritable = WriterProxy.getProxy().lookupObject(e);
        eClass = (Class<E>) e.getClass();
      }

      bodyWriter = bodyWriter.writeValue(eWritable, e);
    }

    return bodyWriter.done();
  }
}

