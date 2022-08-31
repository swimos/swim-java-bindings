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

import ai.swim.structure.value.Value;
import ai.swim.structure.writer.std.ScalarWriters;
import ai.swim.structure.writer.value.ValueWriter;
import org.junit.jupiter.api.Test;

class WriterTest {

  static class Blah {
    public int a;
    public long b;

    public Blah(int a, long b) {
      this.a = a;
      this.b = b;
    }
  }

  static class BlahWriter extends StructuralWritable<Blah> {
    @Override
    public <T> T writeInto(Blah from, Writer<T> writer) {
      HeaderWriter<T> headerWriter = writer.record(0);
      BodyWriter<T> bodyWriter = headerWriter.completeHeader(RecordBodyKind.MapLike, 2);

      return bodyWriter
          .writeSlotWith(ScalarWriters.STRING, "a", ScalarWriters.INT, from.a)
          .writeSlotWith(ScalarWriters.STRING, "b", ScalarWriters.LONG, from.b)
          .done();
    }
  }

  @Test
  void t() {
    Blah blah = new Blah(1, 2);
    BlahWriter blahWriter = new BlahWriter();
    Value value = blahWriter.writeInto(blah, new ValueWriter());
    System.out.println(value);
  }

}