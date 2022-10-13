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

package ai.swim.structure.processor.recognizer.writer.builder;

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.recognizer.RecognizerTransformation;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.CodeBlock;

import static ai.swim.structure.processor.recognizer.writer.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

public class FieldInitializer implements Emitter {

  private final FieldModel fieldModel;
  private final boolean inConstructor;
  private final boolean isAbstract;

  public FieldInitializer(FieldModel fieldModel, boolean inConstructor, boolean isAbstract) {
    this.fieldModel = fieldModel;
    this.inConstructor = inConstructor;
    this.isAbstract = isAbstract;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    return CodeBlock.of("new $L<>($L$L)", FIELD_RECOGNIZING_BUILDER_CLASS, fieldModel.initializer(context, inConstructor, isAbstract), new RecognizerTransformation(fieldModel).emit(context));
  }
}
