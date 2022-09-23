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

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public interface ModelLookup {
  Model lookup(TypeMirror typeMirror, ScopedContext context);

  Model untyped(TypeMirror type);

  /**
   * Produce a model representing a generic class.
   *
   * @param context       scoped context to the root processing element.
   * @param containerType the class type that is bounded by a single generic type parameter. E.g, a raw List class.
   * @param proxy         a recognizer or writer that will be typed by this generic. E.g, a ListRecognizer.
   * @param unrolledType  an unrolled type of the generic type parameter and its model. E.g, <N extends Number> unrolled
   *                      to <Number> and a paired NumberRecognizer.
   * @return a model bounded by this generic type parameter.
   */
  default Model generic(ScopedContext context, TypeMirror containerType, TypeElement proxy, Utils.UnrolledType unrolledType) {
    DeclaredType declaredType = context.getProcessingEnvironment().getTypeUtils().getDeclaredType(proxy, unrolledType.typeMirror);
    return new Model(containerType) {
      @Override
      public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
        return CodeBlock.of("new $T($L)", declaredType, unrolledType.model.initializer(context, inConstructor));
      }
    };
  }

  /**
   * Same as a single generic model but for two generics.
   */
  default Model twoGenerics(ScopedContext context, TypeMirror containerType, TypeElement proxy, Utils.UnrolledType left, Utils.UnrolledType right) {
    DeclaredType declaredType = context.getProcessingEnvironment().getTypeUtils().getDeclaredType(proxy, left.typeMirror, right.typeMirror);
    return new Model(containerType) {
      @Override
      public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
        return CodeBlock.of("new $T($L, $L)", declaredType, left.model.initializer(context, inConstructor), right.model.initializer(context, inConstructor));
      }
    };
  }
}
