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

import ai.swim.structure.annotations.AutoloadedWriter;
import ai.swim.structure.value.Value;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.ValueStructuralWritable;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.WriterException;
import ai.swim.structure.writer.std.ArrayStructuralWritable;
import ai.swim.structure.writer.std.ListStructuralWritable;
import ai.swim.structure.writer.std.MapStructuralWritable;
import ai.swim.structure.writer.std.ScalarWriters;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class WriterProxy {

  private static final WriterProxy INSTANCE = new WriterProxy();
  private final ConcurrentHashMap<Class<?>, WriterFactory<?>> writers;

  private WriterProxy() {
    writers = loadWriters();
  }

  @SuppressWarnings("unchecked")
  private static ConcurrentHashMap<Class<?>, WriterFactory<?>> loadWriters() {
    ConcurrentHashMap<Class<?>, WriterFactory<?>> writers = new ConcurrentHashMap<>();

    writers.put(int[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forInt));
    writers.put(char[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forChar));
    writers.put(long[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forLong));
    writers.put(short[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forShort));
    writers.put(boolean[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forBoolean));
    writers.put(float[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forFloat));
    writers.put(double[].class, WriterFactory.buildFrom(int[].class, Writable.class, ArrayStructuralWritable::forDouble));
    writers.put(Integer.class, WriterFactory.buildFrom(Integer.class, Writable.class, () -> ScalarWriters.INTEGER));
    writers.put(Integer.TYPE, WriterFactory.buildFrom(Integer.TYPE, Writable.class, () -> ScalarWriters.INTEGER));
    writers.put(String.class, WriterFactory.buildFrom(String.class, Writable.class, () -> ScalarWriters.STRING));
    writers.put(Character.class, WriterFactory.buildFrom(Character.class, Writable.class, () -> ScalarWriters.CHARACTER));
    writers.put(Character.TYPE, WriterFactory.buildFrom(Character.TYPE, Writable.class, () -> ScalarWriters.CHARACTER));
    writers.put(Long.class, WriterFactory.buildFrom(Long.class, Writable.class, () -> ScalarWriters.LONG));
    writers.put(Long.TYPE, WriterFactory.buildFrom(Long.TYPE, Writable.class, () -> ScalarWriters.LONG));
    writers.put(Byte.class, WriterFactory.buildFrom(Byte.class, Writable.class, () -> ScalarWriters.BYTE));
    writers.put(Byte[].class, WriterFactory.buildFrom(Byte[].class, Writable.class, () -> ScalarWriters.BOXED_BLOB));
    writers.put(Byte.TYPE, WriterFactory.buildFrom(Byte.TYPE, Writable.class, () -> ScalarWriters.BYTE));
    writers.put(Short.class, WriterFactory.buildFrom(Short.class, Writable.class, () -> ScalarWriters.SHORT));
    writers.put(Short.TYPE, WriterFactory.buildFrom(Short.TYPE, Writable.class, () -> ScalarWriters.SHORT));
    writers.put(Boolean.class, WriterFactory.buildFrom(Boolean.class, Writable.class, () -> ScalarWriters.BOOLEAN));
    writers.put(Boolean.TYPE, WriterFactory.buildFrom(Boolean.TYPE, Writable.class, () -> ScalarWriters.BOOLEAN));
    writers.put(Float.class, WriterFactory.buildFrom(Float.class, Writable.class, () -> ScalarWriters.FLOAT));
    writers.put(Float.TYPE, WriterFactory.buildFrom(Float.TYPE, Writable.class, () -> ScalarWriters.FLOAT));
    writers.put(Double.class, WriterFactory.buildFrom(Double.class, Writable.class, () -> ScalarWriters.DOUBLE));
    writers.put(Double.TYPE, WriterFactory.buildFrom(Double.TYPE, Writable.class, () -> ScalarWriters.DOUBLE));
    writers.put(byte[].class, WriterFactory.buildFrom(byte[].class, Writable.class, () -> ScalarWriters.PRIMITIVE_BLOB));
    writers.put(Number.class, WriterFactory.buildFrom(Number.class, Writable.class, () -> ScalarWriters.NUMBER));
    writers.put(BigInteger.class, WriterFactory.buildFrom(BigInteger.class, Writable.class, () -> ScalarWriters.BIG_INT));
    writers.put(BigDecimal.class, WriterFactory.buildFrom(BigDecimal.class, Writable.class, () -> ScalarWriters.BIG_DECIMAL));
    writers.put(AtomicInteger.class, WriterFactory.buildFrom(AtomicInteger.class, Writable.class, () -> ScalarWriters.ATOMIC_INTEGER));
    writers.put(AtomicLong.class, WriterFactory.buildFrom(AtomicLong.class, Writable.class, () -> ScalarWriters.ATOMIC_LONG));
    writers.put(AtomicBoolean.class, WriterFactory.buildFrom(AtomicBoolean.class, Writable.class, () -> ScalarWriters.ATOMIC_BOOLEAN));
    writers.put(Value.class, WriterFactory.buildFrom(Value.class, StructuralWritable.class, ValueStructuralWritable::new));
    writers.put(Map.class, WriterFactory.buildFrom(Map.class, MapStructuralWritable.class, MapStructuralWritable::new));
    writers.put(Collection.class, WriterFactory.buildFrom(Collection.class, ListStructuralWritable.class, ListStructuralWritable::new));

    loadFromClassPath(writers);

    return writers;
  }

  @SuppressWarnings("unchecked")
  private static void loadFromClassPath(ConcurrentHashMap<Class<?>, WriterFactory<?>> writers) {
    Reflections reflections = new Reflections(ClasspathHelper.forJavaClassPath(), "ai.swim");
    Set<Class<?>> classes = reflections.getTypesAnnotatedWith(AutoloadedWriter.class);

    for (Class<?> clazz : classes) {
      AutoloadedWriter annotation = clazz.getAnnotation(AutoloadedWriter.class);
      Class<?> targetClass = annotation.value();

      if (!Writable.class.isAssignableFrom(targetClass)) {
        String error = String.format("%S is annotated with @%s(%s.class) but %s does not extend %s",
            clazz.getCanonicalName(),
            AutoloadedWriter.class.getSimpleName(),
            targetClass.getCanonicalName(),
            clazz.getCanonicalName(),
            Writable.class.getSimpleName()
        );
        throw new RuntimeException(error);
      }

      if (!clazz.getNestHost().equals(clazz)) {
        if (!Modifier.isStatic(clazz.getModifiers())) {
          throw new RuntimeException("Nested non-static classes are not supported by writers: " + clazz.getCanonicalName());
        }
      }

      try {
        Constructor<?> constructor = clazz.getConstructor();
        Supplier<Writable<?>> supplier = () -> {
          try {
            return (Writable<?>) constructor.newInstance();
          } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Failed to create a new instance of writable '%s'", clazz.getCanonicalName()), e);
          }
        };
        // safety: checked that the class is assignable above
        Class<Writable<?>> typedClass = (Class<Writable<?>>) clazz;
        writers.put(targetClass, WriterFactory.buildFromAny(clazz, typedClass, supplier));
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(String.format("Writable '%s' does not contain a zero-arg constructor", clazz.getCanonicalName()), e);
      }
    }
  }

  public static WriterProxy getProxy() {
    return WriterProxy.INSTANCE;
  }

  public <T> Writable<T> lookupObject(T obj) {
    if (obj == null) {
      throw new NullPointerException();
    }

    @SuppressWarnings("unchecked") Class<T> clazz = (Class<T>) obj.getClass();
    return lookupUntyped(clazz);
  }

  public <T> Writable<T> lookup(Class<T> clazz, WriterTypeParameter<?>... typeParameters) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    if (typeParameters != null && typeParameters.length != 0) {
      return lookupTyped(clazz, typeParameters);
    } else {
      return lookupUntyped(clazz);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Writable<T> lookupUntyped(Class<T> clazz) {
    WriterFactory<T> factory = (WriterFactory<T>) this.writers.get(clazz);

    if (factory == null) {
      return fromStdClassUntyped(clazz);
    }

    return factory.newInstance();
  }

  @SuppressWarnings("unchecked")
  public <T> Writable<T> lookupTyped(Class<T> clazz, WriterTypeParameter<?>... typeParameters) {
    WriterFactory<T> factory = (WriterFactory<T>) this.writers.get(clazz);

    if (factory == null) {
      return fromStdClassTyped(clazz, typeParameters);
    }

    if (!factory.isStructural()) {
      throw new IllegalArgumentException(String.format("%s is not a structural recognizer", clazz.getCanonicalName()));
    }

    return factory.newTypedInstance(typeParameters);
  }

  @SuppressWarnings("unchecked")
  private <T> Writable<T> fromStdClassTyped(Class<T> clazz, WriterTypeParameter<?>... typeParameters) {
    if (Map.class.isAssignableFrom(clazz)) {
      return (Writable<T>) lookupTyped(Map.class, typeParameters);
    }

    if (Collection.class.isAssignableFrom(clazz)) {
      return (Writable<T>) lookupTyped(Collection.class, typeParameters);
    }

    if (clazz.isArray()) {
      return (Writable<T>) arrayType(clazz.getComponentType(), typeParameters);
    }

    throw new WriterException("No writer found for class: " + clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> Writable<T> fromStdClassUntyped(Class<T> clazz) {
    if (Map.class.isAssignableFrom(clazz)) {
      return (Writable<T>) lookupUntyped(Map.class);
    }

    if (Collection.class.isAssignableFrom(clazz)) {
      return (Writable<T>) lookupUntyped(Collection.class);
    }

    if (clazz.isArray()) {
      return (Writable<T>) arrayType(clazz.getComponentType());
    }

    throw new WriterException("No writer found for class: " + clazz);
  }

  public <E> Writable<E[]> arrayType(Class<E> elementType, WriterTypeParameter<?>... typeParameters) {
    if (elementType == null) {
      throw new NullPointerException();
    }

    Writable<E> writable;

    if (typeParameters != null && typeParameters.length != 0) {
      writable = lookupTyped(elementType, typeParameters);
    } else {
      writable = lookupUntyped(elementType);
    }

    return new ArrayStructuralWritable<>(writable);
  }

  public <E, W extends Writable<E>> void register(Class<E> clazz, Class<W> writableClass, Supplier<Writable<?>> supplier) {
    writers.put(clazz, WriterFactory.buildFrom(clazz, writableClass, supplier));
  }

}
