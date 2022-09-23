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

package ai.swim.structure.processor.recognizer.writer.builder.classBuilder;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static ai.swim.structure.processor.recognizer.writer.Lookups.TYPE_PARAMETER;

public class TypeParameterEmitter implements Emitter {
  private final Model model;

  public TypeParameterEmitter(Model model) {
    this.model = model;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement typeElement = elementUtils.getTypeElement(TYPE_PARAMETER);
    TypeMirror erased = typeUtils.erasure(typeElement.asType());

    return CodeBlock.of("$T.from(() -> $L)", erased, model.initializer(context, false));
  }
}
