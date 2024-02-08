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

package ai.swim.structure.recognizer.proxy;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.structural.StructuralRecognizer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

class RecognizerFactory<T> {
  private final Supplier<Recognizer<?>> supplier;
  private final Constructor<Recognizer<?>> typedConstructor;
  private final boolean isStructural;
  private final Class<T> targetClass;

  private RecognizerFactory(Class<T> targetClass,
      Supplier<Recognizer<?>> supplier,
      Constructor<Recognizer<?>> typedConstructor,
      boolean isStructural) {
    this.targetClass = targetClass;
    this.supplier = supplier;
    this.typedConstructor = typedConstructor;
    this.isStructural = isStructural;
  }

  @SuppressWarnings("unchecked")
  public static <T, R extends Recognizer<T>> RecognizerFactory<T> buildFrom(Class<T> targetClass,
      Class<R> recognizerClass,
      Supplier<Recognizer<?>> supplier) {
    Constructor<Recognizer<?>> typedConstructor = null;
    boolean isStructural = false;

    if (StructuralRecognizer.class.isAssignableFrom(recognizerClass)) {
      isStructural = true;

      for (Constructor<?> constructor : recognizerClass.getConstructors()) {
        if (constructor.getAnnotation(AutoForm.TypedConstructor.class) != null) {
          typedConstructor = (Constructor<Recognizer<?>>) constructor;
        }
      }
    }

    return new RecognizerFactory<>(targetClass, supplier, typedConstructor, isStructural);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static RecognizerFactory buildFromAny(Class<?> targetClass,
      Class<? extends Recognizer> clazz,
      Supplier<Recognizer<?>> supplier) {
    return buildFrom(targetClass, clazz, supplier);
  }

  public boolean isStructural() {
    return isStructural;
  }

  @SuppressWarnings("unchecked")
  public Recognizer<T> newInstance() {
    if (supplier == null) {
      throw new RecognizerException(targetClass.getSimpleName() + " requires type parameters to instantiate");
    }

    return (Recognizer<T>) supplier.get();
  }

  @SuppressWarnings("unchecked")
  public Recognizer<T> newTypedInstance(RecognizerTypeParameter<?>... typeParameters) {
    if (typedConstructor == null) {
      throw new IllegalStateException("Not a generic recognizer");
    } else {
      try {
        return (Recognizer<T>) typedConstructor.newInstance((Object[]) typeParameters);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RecognizerException(e);
      }
    }
  }

}
