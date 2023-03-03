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

package ai.swim.structure.processor.recognizer.writer.recognizer;

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static ai.swim.structure.processor.recognizer.writer.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

public class TypeVarFieldInitializer implements Emitter {
  private final FieldModel fieldModel;

  public TypeVarFieldInitializer(FieldModel fieldModel) {
    this.fieldModel = fieldModel;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);
    DeclaredType typedBuilder = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldModel.type(processingEnvironment));

    return CodeBlock.builder().add("new $T($L.build())", typedBuilder, nameFactory.typeParameterName(fieldModel.type(processingEnvironment).toString())).build();
  }
}
