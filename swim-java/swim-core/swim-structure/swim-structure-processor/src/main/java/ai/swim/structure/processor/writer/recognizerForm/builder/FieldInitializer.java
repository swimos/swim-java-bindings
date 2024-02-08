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

package ai.swim.structure.processor.writer.recognizerForm.builder;

import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.RecognizerTransformation;
import com.squareup.javapoet.CodeBlock;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

/**
 * Recognizer field initializer and emitter.
 * <p>
 * Depending on whether the context is scoped to a constructor and depending on the resolved model, this will either
 * emit a field that is directly initialized or initialize the model with the type parameters.
 */
public class FieldInitializer extends Emitter {
  private final FieldModel fieldModel;
  private final boolean inConstructor;
  private final RecognizerContext context;

  public FieldInitializer(FieldModel fieldModel, boolean inConstructor, RecognizerContext context) {
    this.fieldModel = fieldModel;
    this.inConstructor = inConstructor;
    this.context = context;
  }

  @Override
  public String toString() {
    return CodeBlock
        .of(
            "new $L<>($L$L)",
            FIELD_RECOGNIZING_BUILDER_CLASS,
            fieldModel.instantiate(context.getInitializer(), inConstructor),
            new RecognizerTransformation(fieldModel))
        .toString();
  }
}
