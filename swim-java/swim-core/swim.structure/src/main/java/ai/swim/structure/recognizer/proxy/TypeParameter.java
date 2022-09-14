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

package ai.swim.structure.recognizer.proxy;

import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.untyped.UntypedRecognizer;

import java.util.function.Supplier;

public abstract class TypeParameter<T> {

  public static <T> TypeParameter<T> from(Class<T> target) {
    if (target == null) {
      return untyped();
    } else {
      return forClass(target);
    }
  }

  public static <T> TypeParameter<T> from(Supplier<Recognizer<T>> target) {
    if (target == null) {
      return untyped();
    } else {
      return resolved(target);
    }
  }

  public static <T> TypeParameter<T> forClass(Class<T> target) {
    return new GenericTypeParameter<>(target);
  }

  public static <T> TypeParameter<T> resolved(Supplier<Recognizer<T>> recognizer) {
    return new ResolvedParameter<>(recognizer);
  }

  public static <T> TypeParameter<T> untyped() {
    return new UntypedParameter<>();
  }

  public abstract Recognizer<T> build();
}

final class GenericTypeParameter<T> extends TypeParameter<T> {
  private final Class<T> target;

  GenericTypeParameter(Class<T> target) {
    this.target = target;
  }

  @Override
  public Recognizer<T> build() {
    return RecognizerProxy.getProxy().lookup(target);
  }
}

final class UntypedParameter<T> extends TypeParameter<T> {
  @Override
  public Recognizer<T> build() {
    return new UntypedRecognizer<>();
  }
}

final class ResolvedParameter<T> extends TypeParameter<T> {
  private final Supplier<Recognizer<T>> recognizer;

  ResolvedParameter(Supplier<Recognizer<T>> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public Recognizer<T> build() {
    return this.recognizer.get();
  }
}