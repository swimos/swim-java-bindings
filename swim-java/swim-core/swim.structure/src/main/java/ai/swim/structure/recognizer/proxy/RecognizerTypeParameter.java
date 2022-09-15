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

import ai.swim.structure.TypeParameter;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.untyped.UntypedRecognizer;

import java.util.function.Supplier;

public abstract class RecognizerTypeParameter<T> extends TypeParameter<Recognizer<T>> {

  public static <T> RecognizerTypeParameter<T> from(Class<T> target) {
    if (target == null) {
      return untyped();
    } else {
      return forClass(target);
    }
  }

  public static <T> RecognizerTypeParameter<T> from(Supplier<Recognizer<T>> target) {
    if (target == null) {
      return untyped();
    } else {
      return forSupplier(target);
    }
  }

  private static <T> RecognizerTypeParameter<T> forClass(Class<T> target) {
    return forSupplier(() -> RecognizerProxy.getProxy().lookup(target));
  }

  private static <T> RecognizerTypeParameter<T> forSupplier(Supplier<Recognizer<T>> recognizer) {
    return new SupplierParameter<>(recognizer);
  }

  public static <T> RecognizerTypeParameter<T> untyped() {
    return new UntypedParameter<>();
  }
}

final class UntypedParameter<T> extends RecognizerTypeParameter<T> {
  @Override
  public Recognizer<T> build() {
    return new UntypedRecognizer<>();
  }
}

final class SupplierParameter<T> extends RecognizerTypeParameter<T> {
  private final Supplier<Recognizer<T>> recognizer;

  SupplierParameter(Supplier<Recognizer<T>> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public Recognizer<T> build() {
    return this.recognizer.get();
  }
}