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

package ai.swim.structure.writer.proxy;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.proxy.RecognizerTypeParameter;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.Writable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

class WriterFactory<T> {
  private final Supplier<Writable<?>> supplier;
  private final Constructor<Writable<?>> typedConstructor;
  private final boolean isStructural;
  private final Class<T> targetClass;

  WriterFactory(Supplier<Writable<?>> supplier, Constructor<Writable<?>> typedConstructor, boolean isStructural, Class<T> targetClass) {
    this.supplier = supplier;
    this.typedConstructor = typedConstructor;
    this.isStructural = isStructural;
    this.targetClass = targetClass;
  }


  @SuppressWarnings("unchecked")
  public static <T, W extends Writable<T>> WriterFactory<T> buildFrom(Class<T> targetClass, Class<W> writerClass, Supplier<Writable<?>> supplier) {
    Constructor<Writable<?>> typedConstructor = null;
    boolean isStructural = false;

    if (StructuralWritable.class.isAssignableFrom(writerClass)) {
      isStructural = true;

      for (Constructor<?> constructor : writerClass.getConstructors()) {
        if (constructor.getAnnotation(AutoForm.TypedConstructor.class) != null) {
          typedConstructor = (Constructor<Writable<?>>) constructor;
        }
      }
    }

    return new WriterFactory<>(supplier, typedConstructor, isStructural, targetClass);
  }

  public boolean isStructural() {
    return isStructural;
  }

  @SuppressWarnings("unchecked")
  public Writable<T> newInstance() {
    if (supplier == null) {
      throw new RecognizerException(targetClass.getSimpleName() + " requires type parameters to instantiate");
    }

    return (Writable<T>) supplier.get();
  }

  @SuppressWarnings("unchecked")
  public Writable<T> newTypedInstance(WriterTypeParameter<?>... typeParameters) {
    if (typedConstructor == null) {
      throw new IllegalStateException("Not a generic writer");
    } else {
      try {
        return (Writable<T>) typedConstructor.newInstance((Object[]) typeParameters);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RecognizerException(e);
      }
    }
  }

}
