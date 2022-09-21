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

import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.models.RecognizerModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class PolymorphicRecognizer {

  public static final String POLYMORPHIC_RECOGNIZER = "ai.swim.structure.recognizer.structural.PolymorphicRecognizer";

  public static TypeSpec.Builder buildPolymorphicRecognizer(List<RecognizerModel> subTypes, ScopedContext context) {
    AnnotationSpec recognizerAnnotationSpec = AnnotationSpec.builder(AutoloadedRecognizer.class)
        .addMember("value", "$T.class", context.getRoot().asType())
        .build();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getNameFactory().recognizerClassName())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(recognizerAnnotationSpec);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(POLYMORPHIC_RECOGNIZER);
    DeclaredType recognizerType = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    classSpec.superclass(TypeName.get(recognizerType));

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addStatement("super($L)", buildInitializer(subTypes, context))
        .build();
    classSpec.addMethod(constructor);

    return classSpec;
  }

  private static String buildInitializer(List<RecognizerModel> subTypes, ScopedContext context) {
    StringBuilder initializer = new StringBuilder("java.util.List.of(");

    for (int i = 0; i < subTypes.size(); i++) {
      boolean fin = i + 1 >= subTypes.size();
      RecognizerModel recognizerModel = subTypes.get(i);

      initializer.append(recognizerModel.initializer(context, false)).append(fin ? "" : ", ");
    }

    return initializer.append(")").toString();
  }

}
