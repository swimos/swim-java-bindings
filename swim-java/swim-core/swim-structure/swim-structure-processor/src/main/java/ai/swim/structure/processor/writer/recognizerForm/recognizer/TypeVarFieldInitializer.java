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

package ai.swim.structure.processor.writer.recognizerForm.recognizer;

import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import com.squareup.javapoet.CodeBlock;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

/**
 * Generic type variable field initializer and emitter.
 * <p>
 * This is used to initialize a field that accepts a type parameter for its recognizer.
 */
public class TypeVarFieldInitializer extends Emitter {
  private final FieldModel fieldModel;
  private final RecognizerContext context;

  public TypeVarFieldInitializer(RecognizerContext context, FieldModel fieldModel) {
    this.fieldModel = fieldModel;
    this.context = context;
  }

  @Override
  public String toString() {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();

    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);
    DeclaredType typedBuilder = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldModel.type());

    return CodeBlock
        .builder()
        .add("new $T($L.build())", typedBuilder, context.getFormatter().typeParameterName(fieldModel.type().toString()))
        .build()
        .toString();
  }
}
