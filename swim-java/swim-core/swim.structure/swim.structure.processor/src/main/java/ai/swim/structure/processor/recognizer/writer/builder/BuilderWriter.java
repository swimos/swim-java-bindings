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

package ai.swim.structure.processor.recognizer.writer.builder;

import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.Lookups;
import ai.swim.structure.processor.recognizer.writer.builder.classBuilder.ClassBuilder;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.recognizer.writer.builder.header.HeaderBuilder;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class BuilderWriter {

  public static void write(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement classRecognizingBuilderElement = elementUtils.getTypeElement(Lookups.RECOGNIZING_BUILDER_CLASS);
    DeclaredType classRecognizingBuilderType = typeUtils.getDeclaredType(classRecognizingBuilderElement, context.getRoot().asType());

    ClassBuilder classBuilder = new ClassBuilder(schema, context);
    parentSpec.addType(classBuilder.build(TypeName.get(classRecognizingBuilderType)));

    HeaderSet headerSet = schema.getPartitionedFields().headerSet;

    if (!headerSet.headerFields.isEmpty() || headerSet.hasTagBody()) {
      ClassName classType = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());
      TypeElement recognizingBuilderElement = elementUtils.getTypeElement(Lookups.RECOGNIZING_BUILDER_CLASS);
      ParameterizedTypeName headerBuilderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), classType);

      HeaderBuilder headerBuilder = new HeaderBuilder(schema, context);
      parentSpec.addType(headerBuilder.build(headerBuilderType));

      TypeSpec.Builder headerClass = TypeSpec.classBuilder(context.getNameFactory().headerClassName()).addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

      HeaderSet headerFieldSet = schema.getPartitionedFields().headerSet;
      List<FieldModel> headerFields = headerFieldSet.headerFields;

      if (headerFieldSet.tagBody != null) {
        headerFields.add(headerFieldSet.tagBody);
      }

      for (FieldModel headerField : headerFields) {
        headerClass.addField(FieldSpec.builder(TypeName.get(headerField.type(processingEnvironment)), headerField.getName().toString()).build());
      }
      parentSpec.addType(headerClass.build());
    }
  }

}
