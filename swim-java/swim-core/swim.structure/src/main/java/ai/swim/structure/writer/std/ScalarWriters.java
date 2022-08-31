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

import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.Writer;

public class ScalarWriters {
  public final static StructuralWritable<String> STRING = new StructuralWritable<>() {
    @Override
    public <T> T writeInto(String from, Writer<T> writer) {
      return writer.writeText(from);
    }
  };

  public final static StructuralWritable<Integer> INT = new StructuralWritable<>() {
    @Override
    public <T> T writeInto(Integer from, Writer<T> writer) {
      return writer.writeInt(from);
    }
  };

  public final static StructuralWritable<Long> LONG = new StructuralWritable<>() {
    @Override
    public <T> T writeInto(Long from, Writer<T> writer) {
      return writer.writeLong(from);
    }
  };
}
