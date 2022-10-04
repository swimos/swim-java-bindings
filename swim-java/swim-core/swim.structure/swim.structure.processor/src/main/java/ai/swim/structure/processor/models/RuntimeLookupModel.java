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

import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RuntimeLookupModel extends Model {
  private final Model[] parameters;
  private final String tyName;

  public RuntimeLookupModel(String tyName, TypeMirror mirror, Model[] parameters) {
    super(mirror);
    this.tyName = tyName;
    this.parameters = parameters;
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor, boolean isAbstract) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeMirror erasure = typeUtils.erasure(type);

    String typeParameters = "";

    if (parameters != null) {
      typeParameters = Arrays.stream(parameters).map(ty -> {
        return String.format("%s.from(() -> %s)", tyName, ty.initializer(context, inConstructor, isAbstract));
      }).collect(Collectors.joining(", "));
    }

    typeParameters = typeParameters.isBlank() ? "" : ", " + typeParameters;

    if (isAbstract) {
      TypeMirror rootType = context.getRoot().asType();
      return CodeBlock.of("getProxy().lookup((Class<? extends $T>) (Class<?>) $T.class $L)", rootType, erasure, typeParameters);
//      return CodeBlock.of("getProxy().lookup($T.class $L)", erasure, typeParameters);
    } else {
      return CodeBlock.of("getProxy().lookup((Class<$T>) (Class<?>) $T.class $L)", type, erasure, typeParameters);
    }
  }
}
