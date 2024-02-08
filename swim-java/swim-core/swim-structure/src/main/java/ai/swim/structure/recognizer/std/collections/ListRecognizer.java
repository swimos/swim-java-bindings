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

package ai.swim.structure.recognizer.std.collections;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.FirstOf;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleAttrBodyRecognizer;
import java.util.ArrayList;
import java.util.List;

public class ListRecognizer<E> extends CollectionRecognizer<E, List<E>, List<E>> {
  @AutoForm.TypedConstructor
  public ListRecognizer(Recognizer<E> delegate) {
    super(delegate, new ArrayList<>());
  }

  public ListRecognizer(Recognizer<E> delegate, boolean isAttrBody) {
    super(delegate, new ArrayList<>(), isAttrBody);
  }

  @Override
  public Recognizer<List<E>> reset() {
    return new ListRecognizer<>(this.delegate.reset(), isAttrBody);
  }

  @Override
  public Recognizer<List<E>> asAttrRecognizer() {
    return new FirstOf<>(
        new ListRecognizer<>(this.delegate.reset(), true),
        new SimpleAttrBodyRecognizer<>(new ListRecognizer<>(this.delegate.reset(), false))
    );
  }


  @Override
  protected List<E> map(List<E> collection) {
    return collection;
  }
}
