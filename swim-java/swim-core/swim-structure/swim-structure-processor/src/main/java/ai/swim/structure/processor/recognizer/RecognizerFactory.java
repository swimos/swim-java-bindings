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

package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.ModelInstance;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RecognizerFactory {
  private final HashMap<TypeMirror, Model> recognizers;

  private RecognizerFactory(HashMap<TypeMirror, Model> recognizers) {
    this.recognizers = recognizers;
  }

  /**
   * Initialises standard library types.
   */
  public static RecognizerFactory initFrom(ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    HashMap<TypeMirror, Model> recognizers = new HashMap<>();

    // init core types
    ModelInstance.Resolver primitiveResolver = ModelInstance.resolver("ai.swim.structure.recognizer.std.ScalarRecognizer");
    recognizers.put(_getOrThrowType(elementUtils, Byte.class), primitiveResolver.resolve(processingEnvironment, "BYTE"));
    recognizers.put(_getOrThrowType(elementUtils, Short.class), primitiveResolver.resolve(processingEnvironment, "SHORT"));
    recognizers.put(_getOrThrowType(elementUtils, Integer.class), primitiveResolver.resolve(processingEnvironment, "INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, Long.class), primitiveResolver.resolve(processingEnvironment, "LONG"));
    recognizers.put(_getOrThrowType(elementUtils, Float.class), primitiveResolver.resolve(processingEnvironment, "FLOAT"));
    recognizers.put(_getOrThrowType(elementUtils, Boolean.class), primitiveResolver.resolve(processingEnvironment, "BOOLEAN"));
    recognizers.put(_getOrThrowType(elementUtils, String.class), primitiveResolver.resolve(processingEnvironment, "STRING"));
    recognizers.put(_getOrThrowType(elementUtils, Character.class), primitiveResolver.resolve(processingEnvironment, "CHARACTER"));
    recognizers.put(_getOrThrowType(elementUtils, BigInteger.class), primitiveResolver.resolve(processingEnvironment, "BIG_INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, BigDecimal.class), primitiveResolver.resolve(processingEnvironment, "BIG_DECIMAL"));
    recognizers.put(_getOrThrowType(elementUtils, Number.class), primitiveResolver.resolve(processingEnvironment, "NUMBER"));
    recognizers.put(_getOrThrowArrayType(elementUtils, typeUtils, Byte.class), primitiveResolver.resolve(processingEnvironment, "BLOB"));

    // init atomics
    ModelInstance.Resolver atomicResolver = ModelInstance.resolver("ai.swim.structure.recognizer.std.AtomicRecognizer");
    recognizers.put(_getOrThrowType(elementUtils, AtomicBoolean.class), atomicResolver.resolve(processingEnvironment, "ATOMIC_BOOLEAN"));
    recognizers.put(_getOrThrowType(elementUtils, AtomicInteger.class), atomicResolver.resolve(processingEnvironment, "ATOMIC_INTEGER"));
    recognizers.put(_getOrThrowType(elementUtils, AtomicLong.class), atomicResolver.resolve(processingEnvironment, "ATOMIC_LONG"));

    return new RecognizerFactory(recognizers);
  }

  private static <T> TypeMirror _getOrThrowType(Elements elementUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw classInitFailure(clazz);
    }

    return typeElement.asType();
  }

  private static <T> TypeMirror _getOrThrowArrayType(Elements elementUtils, Types typeUtils, Class<T> clazz) {
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());
    if (typeElement == null) {
      throw classInitFailure(clazz);
    }

    ArrayType arrayType = typeUtils.getArrayType(typeElement.asType());

    if (arrayType == null) {
      throw classInitFailure(clazz);
    }

    return arrayType;
  }

  private static RuntimeException classInitFailure(Class<?> clazz) {
    return new RuntimeException("Failed to initialise recognizer factory with class: " + clazz);
  }

  public Model lookup(TypeMirror typeMirror) {
    return this.recognizers.get(typeMirror);
  }

}
