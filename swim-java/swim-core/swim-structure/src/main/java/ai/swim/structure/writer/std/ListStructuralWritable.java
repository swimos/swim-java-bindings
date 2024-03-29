/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.structure.writer.std;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.proxy.WriterTypeParameter;
import java.util.List;

/**
 * A {@code Writable} for writing objects that extend {@code List<T>}.
 *
 * @param <T> the type of the {@code List}'s elements.
 */
public class ListStructuralWritable<T> extends CollectionStructuralWritable<T, List<T>> {
  @AutoForm.TypedConstructor
  public ListStructuralWritable(WriterTypeParameter<T> listWritable) {
    super(listWritable.build(), null);
  }

  public ListStructuralWritable(Writable<T> listWritable, Class<T> tClass) {
    super(listWritable, tClass);
  }

  public ListStructuralWritable() {
    super(null, null);
  }

}
