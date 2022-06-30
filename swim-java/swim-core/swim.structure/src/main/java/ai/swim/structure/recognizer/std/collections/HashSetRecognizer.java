// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.recognizer.std.collections;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.FirstOf;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleAttrBodyRecognizer;

import java.util.HashSet;

public class HashSetRecognizer<E> extends CollectionRecognizer<E, HashSet<E>> {
  @AutoForm.TypedConstructor
  public HashSetRecognizer(Recognizer<E> delegate) {
    super(delegate, new HashSet<>());
  }

  public HashSetRecognizer(Recognizer<E> delegate, boolean isAttrBody) {
    super(delegate, new HashSet<>(), isAttrBody);
  }

  @Override
  public Recognizer<HashSet<E>> reset() {
    return new HashSetRecognizer<>(this.delegate.reset(), isAttrBody);
  }

  @Override
  public Recognizer<HashSet<E>> asAttrRecognizer() {
    return new FirstOf<>(
        new HashSetRecognizer<>(this.delegate.reset(), true),
        new SimpleAttrBodyRecognizer<>(new HashSetRecognizer<>(this.delegate.reset(), false))
    );
  }

}