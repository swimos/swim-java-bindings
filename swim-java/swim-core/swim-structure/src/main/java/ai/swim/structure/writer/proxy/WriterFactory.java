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

package ai.swim.structure.writer.proxy;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.Writable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * A wrapper around {@code Writable<T>} which provides access to creating new writable instances.
 *
 * @param <T> the type that the {@code Writable} produces.
 */
class WriterFactory<T> {
  /**
   * A supplier for creating a new {@code Writable<T>}
   */
  private final Supplier<Writable<?>> supplier;

  /**
   * The constructor to initialise for a {@code Writable} that has generic type parameters.
   */
  private final Constructor<Writable<?>> typedConstructor;

  /**
   * Whether this {@code Writable} is a class-like writable.
   */
  private final boolean isStructural;

  /**
   * The type that this {@code Writable} produces.
   */
  private final Class<T> targetClass;

  WriterFactory(Supplier<Writable<?>> supplier,
      Constructor<Writable<?>> typedConstructor,
      boolean isStructural,
      Class<T> targetClass) {
    this.supplier = supplier;
    this.typedConstructor = typedConstructor;
    this.isStructural = isStructural;
    this.targetClass = targetClass;
  }

  /**
   * Builds a new {@code WriterFactory<T>}
   *
   * @param targetClass the type that this {@code Writable} produces.
   * @param writerClass the type of the {@code Writable} class.
   * @param supplier    for creating new instances.
   * @param <T>         the type that the {@code Writable} produces.
   * @param <W>         the type of the {@code Writable}.
   * @return a factory for {@code Writable<T>}.
   */
  @SuppressWarnings("unchecked")
  public static <T, W extends Writable<T>> WriterFactory<T> buildFrom(Class<T> targetClass,
      Class<W> writerClass,
      Supplier<Writable<?>> supplier) {
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

  /**
   * Builds a new {@code WriterFactory<T>} using wildcard types; useful when working with heterogeneous collections of
   * writable types.
   *
   * @param targetClass the type that this {@code Writable} produces.
   * @param supplier    for creating new instances.
   * @return a factory for {@code Writable<T>}.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static WriterFactory buildFromAny(Class<?> targetClass,
      Class<? extends Writable> clazz,
      Supplier<Writable<?>> supplier) {
    return buildFrom(targetClass, clazz, supplier);
  }

  /**
   * Returns whether this factory provides access to a class-like type.
   */
  public boolean isStructural() {
    return isStructural;
  }

  /**
   * Instantiates a new {@code Writable<T>}.
   */
  @SuppressWarnings("unchecked")
  public Writable<T> newInstance() {
    if (supplier == null) {
      throw new RecognizerException(targetClass.getSimpleName() + " requires type parameters to instantiate");
    }

    return (Writable<T>) supplier.get();
  }

  /**
   * Instantiates a new {@code Writable<T>} initialised with the provided type parameters.
   */
  @SuppressWarnings("unchecked")
  public Writable<T> newTypedInstance(WriterTypeParameter<?>... typeParameters) {
    if (typedConstructor == null) {
      throw new IllegalStateException(String.format("%s is not a generic writer", targetClass));
    } else {
      try {
        return (Writable<T>) typedConstructor.newInstance((Object[]) typeParameters);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RecognizerException(e);
      }
    }
  }

}
