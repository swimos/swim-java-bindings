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

import ai.swim.structure.processor.Utils;
import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;

import static ai.swim.structure.processor.Utils.unrollType;

public abstract class Model {
  protected final TypeMirror type;

  protected Model(TypeMirror type) {
    this.type = type;
  }

  public static Model singleGeneric(ModelLookup modelLookup, TypeElement container, TypeElement proxy, TypeMirror typeMirror, ScopedContext context, boolean isProxy) {
    DeclaredType variableType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = variableType.getTypeArguments();

    if (typeArguments.size() != 1) {
      throw new IllegalArgumentException("Attempted to build a generic type from " + typeArguments.size() + " type parameters where 1 was required");
    }

    TypeMirror listType = typeArguments.get(0);
    Utils.UnrolledType unrolledType = unrollType(modelLookup, context, listType);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();

    if (unrolledType.model == null) {
      return null;
    }

    // retype the container now that we've unrolled the type. I.e, turn List<N extends Number> into List<Number>
    DeclaredType typed = typeUtils.getDeclaredType(container, unrolledType.typeMirror);
    return modelLookup.generic(context, typed, proxy, unrolledType, isProxy);
  }

  public static Model twoGenerics(ModelLookup modelLookup, TypeElement container, TypeElement proxy, TypeMirror typeMirror, ScopedContext context, boolean isProxy) {
    DeclaredType variableType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = variableType.getTypeArguments();

    if (typeArguments.size() != 2) {
      throw new IllegalArgumentException("Attempted to build a generic type from " + typeArguments.size() + " type parameters where 2 were required");
    }

    TypeMirror keyType = typeArguments.get(0);
    TypeMirror valueType = typeArguments.get(1);

    Utils.UnrolledType unrolledKey = unrollType(modelLookup, context, keyType);
    Utils.UnrolledType unrolledValue = unrollType(modelLookup, context, valueType);

    if (unrolledKey.model == null || unrolledValue.model == null) {
      return null;
    }

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();

    // retype the container now that we've unrolled the type. I.e, turn List<N extends Number> into List<Number>
    DeclaredType typed = typeUtils.getDeclaredType(container, unrolledKey.typeMirror, unrolledValue.typeMirror);
    return modelLookup.twoGenerics(context, typed, proxy, unrolledKey, unrolledValue, isProxy);
  }

  public Object defaultValue() {
    return null;
  }

  public abstract CodeBlock initializer(ScopedContext context, boolean inConstructor, boolean isAbstract);

  public TypeMirror type(ProcessingEnvironment environment) {
    return type;
  }

  public boolean isClass() {
    return false;
  }

  public boolean isEnum() {
    return false;
  }

  public boolean isInterface() {
    return false;
  }

  public boolean isRuntimeLookup() {
    return false;
  }
}
