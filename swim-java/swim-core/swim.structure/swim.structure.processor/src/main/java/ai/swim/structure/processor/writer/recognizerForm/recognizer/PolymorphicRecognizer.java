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

package ai.swim.structure.processor.writer.recognizerForm.recognizer;

import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

import static ai.swim.structure.processor.writer.recognizerForm.Lookups.POLYMORPHIC_RECOGNIZER;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZER_CLASS;

/**
 * Abstract class recognizer builder.
 */
public class PolymorphicRecognizer {

  /**
   * Builds an abstract class recognizer for the provided subtypes.
   *
   * @param context  recognizer scoped context to the root processing element.
   * @param subTypes that this recognizer can deserialize into.
   * @return a builder for this recognizer.
   */
  public static TypeSpec.Builder buildPolymorphicRecognizer(RecognizerContext context, List<Model> subTypes) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    DeclaredType declaredType = (DeclaredType) context.getRoot().asType();

    AnnotationSpec recognizerAnnotationSpec = AnnotationSpec.builder(AutoloadedRecognizer.class)
      .addMember("value", "$T.class", typeUtils.erasure(declaredType))
      .build();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getFormatter().recognizerClassName())
      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
      .addAnnotation(recognizerAnnotationSpec);

    for (TypeMirror typeArgument : declaredType.getTypeArguments()) {
      classSpec.addTypeVariable((TypeVariableName) TypeVariableName.get(typeArgument));
    }

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(POLYMORPHIC_RECOGNIZER);
    DeclaredType recognizerType = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    classSpec.superclass(TypeName.get(recognizerType));

    MethodSpec constructor = MethodSpec.constructorBuilder()
      .addModifiers(Modifier.PUBLIC)
      .addStatement("super($L)", buildInitializer(context, subTypes))
      .build();
    classSpec.addMethod(constructor);

    return classSpec;
  }

  private static String buildInitializer(RecognizerContext context, List<Model> subTypes) {
    StringBuilder initializer = new StringBuilder("java.util.List.of(");

    for (int i = 0; i < subTypes.size(); i++) {
      boolean fin = i + 1 >= subTypes.size();
      Model recognizerModel = subTypes.get(i);

      TypeMirror superType = context.getRoot().asType();
      CodeBlock init = recognizerModel.instantiate(context.getInitializer(), false).getInitializer();
      CodeBlock cast = CodeBlock.of("($L<? extends $T>)", RECOGNIZER_CLASS, superType);
      initializer.append(CodeBlock.of("$L $L", cast, init)).append(fin ? "" : ", ");
    }

    return initializer.append(")").toString();
  }

}
