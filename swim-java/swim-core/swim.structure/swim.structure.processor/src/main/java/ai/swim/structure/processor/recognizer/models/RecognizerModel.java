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

import ai.swim.structure.processor.recognizer.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.Utils.isSubType;

public abstract class RecognizerModel {

  protected final TypeMirror type;

  protected RecognizerModel(TypeMirror type) {
    this.type = type;
  }

  public Object defaultValue() {
    return null;
  }

  public TypeMirror type(ProcessingEnvironment environment) {
    return type;
  }

  public abstract CodeBlock initializer(ScopedContext context, boolean inConstructor);

  public static RecognizerModel runtimeLookup(TypeMirror type) {
    return new RuntimeLookup(type, null);
  }

  public static RecognizerModel runtimeLookup(TypeMirror type, RecognizerModel[] parameters) {
    return new RuntimeLookup(type, parameters);
  }

  public static RecognizerModel untyped(TypeMirror type) {
    return new UntypedRecognizer(type);
  }

  public static RecognizerModel from(TypeMirror typeMirror, ScopedContext context) {
    RecognizerModel recognizer = RecognizerModel.fromPrimitiveType(typeMirror);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = RecognizerModel.fromStdType(typeMirror, context);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = context.getRecognizerFactory().lookup(typeMirror);

    if (recognizer != null) {
      return recognizer;
    }

    switch (typeMirror.getKind()) {
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) typeMirror;

        if (declaredType.getTypeArguments().isEmpty()) {
          return runtimeLookup(typeMirror);
        } else {
          RecognizerModel[] typeParameters = declaredType.getTypeArguments().stream().map(ty -> RecognizerModel.from(ty, context)).collect(Collectors.toList()).toArray(RecognizerModel[]::new);

          return runtimeLookup(typeMirror, typeParameters);
        }
      case TYPEVAR:
        return untyped(typeMirror);
      case WILDCARD:
        throw new AssertionError("Recognizer model wildcard type");
      default:
        // We're out of options now. The recognizer isn't available to us now, so we'll have to hope that it's been
        // registered with the recognizer proxy for a runtime lookup.
        return runtimeLookup(typeMirror);
    }
  }

  private static RecognizerModel fromStdType(TypeMirror mirror, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();

    if (isSubType(processingEnvironment, mirror, Collection.class)) {
      return ListRecognizerModel.from(mirror, context);
    }

    if (isSubType(processingEnvironment, mirror, Map.class)) {
      return MapRecognizerModel.from(mirror, context);
    }

    return null;
  }

  public static RecognizerModel fromPrimitiveType(TypeMirror mirror) {
    TypeKind kind = mirror.getKind();
    switch (kind) {
      case BOOLEAN:
        return PrimitiveRecognizerModel.booleanRecognizer();
      case BYTE:
        return PrimitiveRecognizerModel.byteRecognizer();
      case SHORT:
        return PrimitiveRecognizerModel.shortRecognizer();
      case INT:
        return PrimitiveRecognizerModel.intRecognizer();
      case LONG:
        return PrimitiveRecognizerModel.longRecognizer();
      case CHAR:
        return PrimitiveRecognizerModel.charRecognizer();
      case FLOAT:
        return PrimitiveRecognizerModel.floatRecognizer();
      case DOUBLE:
        return PrimitiveRecognizerModel.doubleRecognizer();
      default:
        return null;
    }
  }

}
