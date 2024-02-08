/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.structure.processor.writer.recognizerForm;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.InterfaceModel;
import ai.swim.structure.processor.model.ModelInspector;
import ai.swim.structure.processor.writer.Writer;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.PolymorphicRecognizer;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.Recognizer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZER_PROXY;

public class RecognizerFormWriter implements Writer {
  private final ProcessingEnvironment environment;
  private final ModelInspector inspector;

  public RecognizerFormWriter(ProcessingEnvironment environment, ModelInspector inspector) {
    this.environment = environment;
    this.inspector = inspector;
  }

  public static List<ParameterSpec> writeGenericRecognizerConstructor(List<? extends TypeParameterElement> typeParameters,
      RecognizerContext context) {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();

    List<ParameterSpec> parameters = new ArrayList<>(typeParameters.size());
    TypeElement typeParameterElement = elementUtils.getTypeElement(Lookups.TYPE_PARAMETER);

    for (TypeParameterElement typeParameter : typeParameters) {
      DeclaredType typed = typeUtils.getDeclaredType(typeParameterElement, typeParameter.asType());
      parameters.add(ParameterSpec
                         .builder(
                             TypeName.get(typed),
                             context.getFormatter().typeParameterName(typeParameter.toString()))
                         .build());
    }

    return parameters;
  }

  @Override
  public void writeClass(ClassLikeModel model) throws IOException {
    RecognizerContext context = RecognizerContext.build(
        model.getElement(),
        environment,
        inspector,
        model.getJavaClassName(),
        model.getDeclaredPackage());
    Recognizer.writeRecognizer(model, context);
  }

  @Override
  public void writeInterface(InterfaceModel model) throws IOException {
    RecognizerContext context = RecognizerContext.build(
        model.getElement(),
        environment,
        inspector,
        model.getJavaClassName(),
        model.getDeclaredPackage());
    TypeSpec typeSpec = PolymorphicRecognizer.buildPolymorphicRecognizer(context, model.getSubTypes())
        .build();
    JavaFile javaFile = JavaFile.builder(model.getDeclaredPackage().getQualifiedName().toString(), typeSpec)
        .addStaticImport(ClassName.bestGuess(RECOGNIZER_PROXY), "getProxy")
        .build();

    javaFile.writeTo(context.getProcessingEnvironment().getFiler());
  }

}
