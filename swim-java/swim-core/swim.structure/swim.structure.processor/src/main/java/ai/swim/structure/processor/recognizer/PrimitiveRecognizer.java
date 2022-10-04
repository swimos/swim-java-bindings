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

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.Model;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public class PrimitiveRecognizer<T> extends Model {

  private static Model BYTE_RECOGNIZER;
  private static Model BOOLEAN_RECOGNIZER;
  private static Model SHORT_RECOGNIZER;
  private static Model INT_RECOGNIZER;
  private static Model LONG_RECOGNIZER;
  private static Model CHAR_RECOGNIZER;
  private static Model FLOAT_RECOGNIZER;
  private static Model DOUBLE_RECOGNIZER;
  private final T defaultValue;
  private final String type;

  public PrimitiveRecognizer(String type, T defaultValue) {
    super(null);
    this.type = type;
    this.defaultValue = defaultValue;
  }

  public static Model booleanRecognizer() {
    if (BOOLEAN_RECOGNIZER == null) {
      BOOLEAN_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.BOOLEAN", false);
    }

    return BOOLEAN_RECOGNIZER;
  }

  public static Model byteRecognizer() {
    if (BYTE_RECOGNIZER == null) {
      BYTE_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.BYTE", (byte) 0);
    }

    return BYTE_RECOGNIZER;
  }

  public static Model shortRecognizer() {
    if (SHORT_RECOGNIZER == null) {
      SHORT_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.SHORT", (short) 0);
    }

    return SHORT_RECOGNIZER;
  }

  public static Model intRecognizer() {
    if (INT_RECOGNIZER == null) {
      INT_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.INTEGER", 0);
    }

    return INT_RECOGNIZER;
  }

  public static Model longRecognizer() {
    if (LONG_RECOGNIZER == null) {
      LONG_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.LONG", 0L);
    }

    return LONG_RECOGNIZER;
  }

  public static Model charRecognizer() {
    if (CHAR_RECOGNIZER == null) {
      CHAR_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.CHARACTER", '\u0000');
    }

    return CHAR_RECOGNIZER;
  }

  public static Model floatRecognizer() {
    if (FLOAT_RECOGNIZER == null) {
      FLOAT_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.FLOAT", 0f);
    }

    return FLOAT_RECOGNIZER;
  }

  public static Model doubleRecognizer() {
    if (DOUBLE_RECOGNIZER == null) {
      DOUBLE_RECOGNIZER = new PrimitiveRecognizer<>("ai.swim.structure.recognizer.std.ScalarRecognizer.DOUBLE", 0d);
    }

    return DOUBLE_RECOGNIZER;
  }

  @Override
  public String toString() {
    return "PrimitiveModel{" + "defaultValue=" + defaultValue + ", type=" + type + '}';
  }

  @Override
  public Object defaultValue() {
    return this.defaultValue;
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return environment.getElementUtils().getTypeElement(defaultValue.getClass().getCanonicalName()).asType();
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor, boolean isAbstract) {
    return CodeBlock.of("$L", type);
  }

}
