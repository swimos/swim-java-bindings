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

package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.ClassMap;
import ai.swim.structure.processor.models.InterfaceMap;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.RuntimeLookupModel;
import ai.swim.structure.processor.models.UntypedModel;
import ai.swim.structure.processor.recognizer.writer.recognizer.PolymorphicRecognizer;
import ai.swim.structure.processor.recognizer.writer.recognizer.Recognizer;
import ai.swim.structure.processor.schema.ClassSchema;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.Utils.isSubType;
import static ai.swim.structure.processor.recognizer.writer.Lookups.LIST_RECOGNIZER_CLASS;
import static ai.swim.structure.processor.recognizer.writer.Lookups.MAP_RECOGNIZER_CLASS;
import static ai.swim.structure.processor.recognizer.writer.Lookups.RECOGNIZER_PROXY;
import static ai.swim.structure.processor.recognizer.writer.Lookups.UNTYPED_RECOGNIZER;

public class RecognizerModel {

  public static final String RUNTIME_LOOKUP = "RecognizerTypeParameter";

  public static Model from(TypeMirror typeMirror, ScopedContext context) {
    Model recognizer = RecognizerModel.fromPrimitiveType(typeMirror);

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
          return new RuntimeLookupModel(RUNTIME_LOOKUP, typeMirror, null);
        } else {
          Model[] typeParameters = declaredType.getTypeArguments().stream().map(ty -> RecognizerModel.from(ty, context)).collect(Collectors.toList()).toArray(Model[]::new);
          return new RuntimeLookupModel(RUNTIME_LOOKUP, typeMirror, typeParameters);
        }
      case TYPEVAR:
        return new UntypedModel(typeMirror, UNTYPED_RECOGNIZER);
      case WILDCARD:
        throw new AssertionError("Recognizer model wildcard type");
      default:
        // We're out of options now. The recognizer isn't available to us now, so we'll have to hope that it's been
        // registered with the recognizer proxy for a runtime lookup.
        return new RuntimeLookupModel(RUNTIME_LOOKUP, typeMirror, null);
    }
  }

  private static Model fromStdType(TypeMirror mirror, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    if (isSubType(processingEnvironment, mirror, Collection.class)) {
      return Model.singleGeneric(
          new RecognizerModelLookup(),
          elementUtils.getTypeElement(List.class.getCanonicalName()),
          elementUtils.getTypeElement(LIST_RECOGNIZER_CLASS),
          mirror,
          context
      );
    }

    if (isSubType(processingEnvironment, mirror, Map.class)) {
      return Model.twoGenerics(
          new RecognizerModelLookup(),
          elementUtils.getTypeElement(Map.class.getCanonicalName()),
          elementUtils.getTypeElement(MAP_RECOGNIZER_CLASS),
          mirror,
          context
      );
    }

    return null;
  }

  public static Model fromPrimitiveType(TypeMirror mirror) {
    TypeKind kind = mirror.getKind();
    switch (kind) {
      case BOOLEAN:
        return PrimitiveRecognizer.booleanRecognizer();
      case BYTE:
        return PrimitiveRecognizer.byteRecognizer();
      case SHORT:
        return PrimitiveRecognizer.shortRecognizer();
      case INT:
        return PrimitiveRecognizer.intRecognizer();
      case LONG:
        return PrimitiveRecognizer.longRecognizer();
      case CHAR:
        return PrimitiveRecognizer.charRecognizer();
      case FLOAT:
        return PrimitiveRecognizer.floatRecognizer();
      case DOUBLE:
        return PrimitiveRecognizer.doubleRecognizer();
      default:
        return null;
    }
  }

  public static void write(Model model, ScopedContext context) throws IOException {
    if (model.isClass()) {
      ClassSchema classSchema = ClassSchema.fromMap((ClassMap) model);
      Recognizer.writeRecognizer(classSchema, context);
    } else if (model.isInterface()) {
      InterfaceMap interfaceMap = (InterfaceMap) model;
      TypeSpec typeSpec = PolymorphicRecognizer.buildPolymorphicRecognizer(interfaceMap.getSubTypes(), context)
          .build();
      JavaFile javaFile = JavaFile.builder(interfaceMap.getDeclaredPackage().getQualifiedName().toString(), typeSpec)
          .addStaticImport(ClassName.bestGuess(RECOGNIZER_PROXY), "getProxy")
          .build();

      javaFile.writeTo(context.getProcessingEnvironment().getFiler());
    } else {
      throw new RuntimeException("Unimplemented schema type: " + model);
    }
  }
}
