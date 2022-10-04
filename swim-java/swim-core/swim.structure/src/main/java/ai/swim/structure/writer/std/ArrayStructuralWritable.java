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

import ai.swim.structure.writer.BodyWriter;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.Writable;

public class ArrayStructuralWritable<E> implements StructuralWritable<E[]> {
  private final Writable<E> writable;

  public ArrayStructuralWritable(Writable<E> writable) {
    this.writable = writable;
  }

  @Override
  public <T> T writeInto(E[] from, StructuralWriter<T> structuralWriter) {
    int len = from.length;
    BodyWriter<T> bodyWriter = structuralWriter.record(0).completeHeader(len);

    for (E elem : from) {
      bodyWriter = bodyWriter.writeValueWith(writable, elem);
    }

    return bodyWriter.done();
  }
}
