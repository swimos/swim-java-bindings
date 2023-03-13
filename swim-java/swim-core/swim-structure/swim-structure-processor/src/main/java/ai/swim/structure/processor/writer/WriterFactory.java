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

package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.ModelInstance;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WriterFactory {
  private final HashMap<TypeMirror, Model> writers;

  private WriterFactory(HashMap<TypeMirror, Model> writers) {
    this.writers = writers;
  }

  /**
   * Initialises standard library types.
   */
  public static WriterFactory initFrom(ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    HashMap<TypeMirror, Model> writers = new HashMap<>();

    // init core types
    ModelInstance.Resolver resolver = ModelInstance.resolver("ai.swim.structure.writer.std.ScalarWriters");
    writers.put(_getOrThrowType(elementUtils, Byte.class), resolver.resolve(processingEnvironment, "BYTE"));
    writers.put(_getOrThrowType(elementUtils, Short.class), resolver.resolve(processingEnvironment, "SHORT"));
    writers.put(_getOrThrowType(elementUtils, Integer.class), resolver.resolve(processingEnvironment, "INTEGER"));
    writers.put(_getOrThrowType(elementUtils, Long.class), resolver.resolve(processingEnvironment, "LONG"));
    writers.put(_getOrThrowType(elementUtils, Float.class), resolver.resolve(processingEnvironment, "FLOAT"));
    writers.put(_getOrThrowType(elementUtils, Boolean.class), resolver.resolve(processingEnvironment, "BOOLEAN"));
    writers.put(_getOrThrowType(elementUtils, String.class), resolver.resolve(processingEnvironment, "STRING"));
    writers.put(_getOrThrowType(elementUtils, Character.class), resolver.resolve(processingEnvironment, "CHARACTER"));
    writers.put(_getOrThrowType(elementUtils, Number.class), resolver.resolve(processingEnvironment, "NUMBER"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.BYTE), resolver.resolve(processingEnvironment, "BYTE"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.SHORT), resolver.resolve(processingEnvironment, "SHORT"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.INT), resolver.resolve(processingEnvironment, "INTEGER"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.LONG), resolver.resolve(processingEnvironment, "LONG"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.FLOAT), resolver.resolve(processingEnvironment, "FLOAT"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.BOOLEAN), resolver.resolve(processingEnvironment, "BOOLEAN"));
    writers.put(typeUtils.getPrimitiveType(TypeKind.CHAR), resolver.resolve(processingEnvironment, "CHARACTER"));
    writers.put(_getOrThrowType(elementUtils, BigInteger.class), resolver.resolve(processingEnvironment, "BIG_INT"));
    writers.put(_getOrThrowType(elementUtils, BigDecimal.class), resolver.resolve(processingEnvironment, "BIG_DECIMAL"));
    writers.put(_getOrThrowArrayType(elementUtils, typeUtils, Byte.class), resolver.resolve(processingEnvironment, "BOXED_BLOB"));
    writers.put(_getOrThrowArrayType(typeUtils, TypeKind.BYTE), resolver.resolve(processingEnvironment, "PRIMITIVE_BLOB"));

    // init atomics
    writers.put(_getOrThrowType(elementUtils, AtomicBoolean.class), resolver.resolve(processingEnvironment, "ATOMIC_BOOLEAN"));
    writers.put(_getOrThrowType(elementUtils, AtomicInteger.class), resolver.resolve(processingEnvironment, "ATOMIC_INTEGER"));
    writers.put(_getOrThrowType(elementUtils, AtomicLong.class), resolver.resolve(processingEnvironment, "ATOMIC_LONG"));

    return new WriterFactory(writers);
  }

  private static <T> TypeMirror _getOrThrowType(Elements elementUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw classInitFailure(clazz.getCanonicalName());
    }

    return typeElement.asType();
  }

  private static <T> TypeMirror _getOrThrowArrayType(Elements elementUtils, Types typeUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw classInitFailure(clazz.getCanonicalName());
    }

    ArrayType arrayType = typeUtils.getArrayType(typeElement.asType());

    if (arrayType == null) {
      throw classInitFailure(clazz.getCanonicalName());
    }

    return arrayType;
  }

  private static TypeMirror _getOrThrowArrayType(Types typeUtils, TypeKind typeKind) {
    PrimitiveType type = typeUtils.getPrimitiveType(typeKind);
    ArrayType arrayType = typeUtils.getArrayType(type);

    if (arrayType == null) {
      throw classInitFailure(typeKind.toString());
    }

    return arrayType;
  }

  private static RuntimeException classInitFailure(String elem) {
    return new RuntimeException("Failed to initialise writer factory with: " + elem);
  }

  public Model lookup(TypeMirror typeMirror) {
    return this.writers.get(typeMirror);
  }

}
