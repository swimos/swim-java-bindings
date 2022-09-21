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

package ai.swim.structure.processor.recognizer.models;

import ai.swim.structure.processor.Utils;
import ai.swim.structure.processor.recognizer.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

import static ai.swim.structure.processor.Utils.unrollType;
import static ai.swim.structure.processor.recognizer.writer.Lookups.LIST_RECOGNIZER_CLASS;

public class ListRecognizerModel extends StructuralRecognizer {
  private final RecognizerModel delegate;
  private final TypeMirror listType;

  private ListRecognizerModel(TypeMirror type, RecognizerModel delegate, TypeMirror listType) {
    super(type);
    this.delegate = delegate;
    this.listType = listType;
  }

  public static StructuralRecognizer from(TypeMirror typeMirror, ScopedContext context) {
    DeclaredType variableType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = variableType.getTypeArguments();

    if (typeArguments.size() != 1) {
      throw new IllegalArgumentException("Attempted to build a list from " + typeArguments.size() + " type parameters");
    }

    TypeMirror listType = typeArguments.get(0);
    Utils.UnrolledType unrolledType = unrollType(context, listType);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement listTypeElement = elementUtils.getTypeElement(List.class.getCanonicalName());
    DeclaredType typedList = typeUtils.getDeclaredType(listTypeElement, unrolledType.typeMirror);

    return new ListRecognizerModel(typedList, unrolledType.recognizerModel, unrolledType.typeMirror);
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement typeElement = elementUtils.getTypeElement(LIST_RECOGNIZER_CLASS);
    DeclaredType declaredType = typeUtils.getDeclaredType(typeElement, listType);

    return CodeBlock.of("new $T($L)", declaredType, this.delegate.initializer(context, inConstructor));
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return this.type;
  }

}
