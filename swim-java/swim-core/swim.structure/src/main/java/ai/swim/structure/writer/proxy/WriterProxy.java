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

import ai.swim.structure.value.Value;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.ValueStructuralWritable;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.std.ScalarWriters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

public class WriterProxy {

  private static final WriterProxy INSTANCE = new WriterProxy();
  private final ConcurrentHashMap<Class<?>, WriterFactory<?>> writers;

  private WriterProxy() {
    writers = loadWriters();
  }

  @SuppressWarnings("unchecked")
  private static ConcurrentHashMap<Class<?>, WriterFactory<?>> loadWriters() {
    ConcurrentHashMap<Class<?>, WriterFactory<?>> writers = new ConcurrentHashMap<>();

    writers.put(Integer.class, WriterFactory.buildFrom(Integer.class, Writable.class, () -> ScalarWriters.INTEGER));
    writers.put(String.class, WriterFactory.buildFrom(String.class, Writable.class, () -> ScalarWriters.STRING));
    writers.put(Character.class, WriterFactory.buildFrom(Character.class, Writable.class, () -> ScalarWriters.CHARACTER));
    writers.put(Long.class, WriterFactory.buildFrom(Long.class, Writable.class, () -> ScalarWriters.LONG));
    writers.put(Byte.class, WriterFactory.buildFrom(Byte.class, Writable.class, () -> ScalarWriters.BYTE));
    writers.put(Short.class, WriterFactory.buildFrom(Short.class, Writable.class, () -> ScalarWriters.SHORT));
    writers.put(Boolean.class, WriterFactory.buildFrom(Boolean.class, Writable.class, () -> ScalarWriters.BOOLEAN));
    writers.put(Float.class, WriterFactory.buildFrom(Float.class, Writable.class, () -> ScalarWriters.FLOAT));
    writers.put(Double.class, WriterFactory.buildFrom(Double.class, Writable.class, () -> ScalarWriters.DOUBLE));
    writers.put(byte[].class, WriterFactory.buildFrom(byte[].class, Writable.class, () -> ScalarWriters.BLOB));
    writers.put(BigInteger.class, WriterFactory.buildFrom(BigInteger.class, Writable.class, () -> ScalarWriters.BIG_INT));
    writers.put(BigDecimal.class, WriterFactory.buildFrom(BigDecimal.class, Writable.class, () -> ScalarWriters.BIG_DECIMAL));
    writers.put(Value.class, WriterFactory.buildFrom(Value.class, StructuralWritable.class, ValueStructuralWritable::new));

    return writers;
  }

  public static <K> Writable<K> lookup(Class<?> clazz) {
    throw new AssertionError("Unimplemented");
  }

  public static WriterProxy getProxy() {
    return WriterProxy.INSTANCE;
  }
}
