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

package ai.swim.structure.processor.models;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class UntypedModel extends StructuralModel {

  private final String untypedModel;

  public UntypedModel(TypeMirror typeMirror, String untypedModel) {
    super(typeMirror);
    this.untypedModel = untypedModel;
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor, boolean isAbstract) {
    if (inConstructor) {
      NameFactory nameFactory = context.getNameFactory();
      TypeVariable typeVariable = (TypeVariable) type;
      return CodeBlock.of("$L.build()", nameFactory.typeParameterName(typeVariable.toString()));
    } else {
      ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
      Elements elementUtils = processingEnvironment.getElementUtils();
      Types typeUtils = processingEnvironment.getTypeUtils();

      TypeElement typeElement = elementUtils.getTypeElement(untypedModel);
      DeclaredType declaredType = typeUtils.getDeclaredType(typeElement, type);

      return CodeBlock.of("new $T()", declaredType);
    }
  }

}
