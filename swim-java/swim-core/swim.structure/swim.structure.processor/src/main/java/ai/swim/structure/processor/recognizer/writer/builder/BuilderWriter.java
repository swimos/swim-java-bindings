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

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.Lookups;
import ai.swim.structure.processor.recognizer.writer.builder.classBuilder.ClassBuilder;
import ai.swim.structure.processor.recognizer.writer.builder.header.HeaderBuilder;
import ai.swim.structure.processor.recognizer.writer.recognizer.Recognizer;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ai.swim.structure.processor.recognizer.writer.WriterUtils.typeParametersToTypeVariable;

public class BuilderWriter {

  public static void write(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context, Recognizer.Transposition transposition) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement classRecognizingBuilderElement = elementUtils.getTypeElement(Lookups.RECOGNIZING_BUILDER_CLASS);
    ParameterizedTypeName classRecognizingBuilderType = ParameterizedTypeName.get(ClassName.get(classRecognizingBuilderElement), transposition.builderType(context));

    ClassBuilder classBuilder = new ClassBuilder(schema, context, transposition);
    parentSpec.addType(classBuilder.build(classRecognizingBuilderType));

    HeaderSet headerSet = schema.getPartitionedFields().headerSet;

    if (!headerSet.headerFields.isEmpty() || headerSet.hasTagBody()) {
      TypeElement recognizingBuilderElement = elementUtils.getTypeElement(Lookups.RECOGNIZING_BUILDER_CLASS);
      List<TypeVariableName> mappedTypeParameters = typeParametersToVariables(headerSet.typeParameters(), schema.getTypeParameters(), context.getRoot());

      ParameterizedTypeName recognizingBuilderType;

      if (mappedTypeParameters.isEmpty()) {
        recognizingBuilderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), ClassName.bestGuess(context.getNameFactory().headerCanonicalName()));
      } else {
        ParameterizedTypeName typedHeader = ParameterizedTypeName.get(ClassName.bestGuess(context.getNameFactory().headerCanonicalName()), mappedTypeParameters.toArray(TypeName[]::new));
        recognizingBuilderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), typedHeader);
      }

      HeaderBuilder headerBuilder = new HeaderBuilder(schema, context, mappedTypeParameters);
      parentSpec.addType(headerBuilder.build(recognizingBuilderType));

      TypeSpec.Builder headerClass = TypeSpec.classBuilder(context.getNameFactory().headerClassName())
          .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
          .addTypeVariables(mappedTypeParameters);

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

  /**
   * Maps a subset of type parameters to type variable names that can then be used to define a new parameterized class.
   * <p>
   * This is useful when processing a parameterized class that has promoted generic fields to headers and a new generic
   * header class must be defined. Using this method, the field type mirrors can be looked up against the type parameter
   * elements from the root processing element and then added to the header class.
   * <p>
   *
   * @param typeParameters the subset of type mirrors to map.
   * @param rootParameters a collection of type parameter elements that will be used as a lookup.
   * @param root           the root processing element.
   * @return a mapped list of type variable names.
   */
  public static List<TypeVariableName> typeParametersToVariables(Collection<TypeMirror> typeParameters, Collection<? extends TypeParameterElement> rootParameters, Element root) {
    List<TypeParameterElement> mappedTypeParameters = new ArrayList<>(typeParameters.size());

    for (TypeMirror headerTypeParameter : typeParameters) {
      for (TypeParameterElement schemaTy : rootParameters) {
        if (schemaTy.asType().equals(headerTypeParameter)) {
          mappedTypeParameters.add(schemaTy);
        }
      }
    }

    if (typeParameters.size() != mappedTypeParameters.size()) {
      throw new RuntimeException("Bug: failed to correctly map type arguments for " + root);
    }

    return typeParametersToTypeVariable(mappedTypeParameters);
  }

}
