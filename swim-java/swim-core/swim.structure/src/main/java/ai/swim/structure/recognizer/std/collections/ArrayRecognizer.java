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

package ai.swim.structure.recognizer.std.collections;


import ai.swim.structure.recognizer.FirstOf;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleAttrBodyRecognizer;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayRecognizer<E> extends CollectionRecognizer<E, List<E>, E[]> {
  private final Class<E> clazz;

  public ArrayRecognizer(Class<E> clazz, Recognizer<E> delegate) {
    super(delegate, new ArrayList<>());
    this.clazz = clazz;
  }

  public ArrayRecognizer(Class<E> clazz, Recognizer<E> delegate, boolean isAttrBody) {
    super(delegate, new ArrayList<>(), isAttrBody);
    this.clazz = clazz;
  }

  @Override
  public Recognizer<E[]> reset() {
    return new ArrayRecognizer<>(clazz, super.delegate.reset());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected E[] map(List<E> collection) {
    return collection.toArray((E[]) Array.newInstance(clazz, collection.size()));
  }

  @Override
  public Recognizer<E[]> asAttrRecognizer() {
    return new FirstOf<>(
      new ArrayRecognizer<>(clazz, super.delegate.reset(), true),
      new SimpleAttrBodyRecognizer<>(new ArrayRecognizer<>(clazz, super.delegate.reset(), false))
    );
  }

}
