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

package ai.swim.structure.value;

import ai.swim.structure.writer.PrimitiveWriter;

public abstract class PrimitiveValue extends Value{
  @Override
  public boolean isPrimitive() {
    return true;
  }

  public <T> T visitPrimitiveWritable(PrimitiveWriter<T> writer) {
    if (!isPrimitive()) {
      throw new IllegalStateException("Attempted to visit a non-primitive value type");
    }
    return writePrimitive(writer);
  }

  protected abstract <T> T writePrimitive(PrimitiveWriter<T> writer);
}
