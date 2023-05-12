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

package ai.swim.structure.processor.writer.recognizerForm.builder.classBuilder;

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerNameFormatter;
import ai.swim.structure.processor.writer.recognizerForm.builder.Builder;
import ai.swim.structure.processor.writer.recognizerForm.builder.FieldInitializer;
import ai.swim.structure.processor.writer.recognizerForm.builder.header.HeaderIndexFn;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.Recognizer;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.TypeVarFieldInitializer;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.swim.structure.processor.writer.WriterUtils.typeParametersToTypeVariable;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.writer.recognizerForm.RecognizerFormWriter.writeGenericRecognizerConstructor;

public class ClassBuilder extends Builder {

  private final Recognizer.Transposition transposition;

  public ClassBuilder(ClassLikeModel model, PartitionedFields fields, RecognizerContext context, Recognizer.Transposition transposition) {
    super(model, fields, context);
    this.transposition = transposition;
  }

  @Override
  protected TypeSpec.Builder init() {
    return TypeSpec.classBuilder(context.getFormatter().builderClassName())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .addTypeVariables(typeParametersToTypeVariable(model.getTypeParameters()));
  }

  @Override
  protected List<MethodSpec> buildConstructors() {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    List<ParameterSpec> parameters = writeGenericRecognizerConstructor(model.getTypeParameters(), context);
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldDiscriminate discriminate : partitionedFields.discriminate()) {
      if (!discriminate.isHeader()) {
        FieldModel fieldModel = discriminate.getSingleField();
        VariableElement element = fieldModel.getElement();
        TypeKind typeKind = element.asType().getKind();

        if (typeKind.equals(TypeKind.TYPEVAR)) {
          body.add(initialiseTypeVarField(context, fieldModel));
        } else {
          body.add(initialiseParameterisedField(context, fieldModel));
        }
      }
    }

    if (partitionedFields.hasHeaderFields()) {
      RecognizerNameFormatter formatter = context.getFormatter();
      HashSet<TypeMirror> headerTypeParameters = partitionedFields.headerSet.typeParameters();
      String typeParameters = headerTypeParameters.stream().map(ty -> formatter.typeParameterName(ty.toString())).collect(Collectors.joining(", "));

      body.add(CodeBlock.of(
              "this.$L = $L($L, () -> new $L($L), $L, $L);",
              formatter.headerBuilderFieldName(),
              formatter.headerBuilderMethod(),
              partitionedFields.headerSet.hasTagBody(),
              formatter.headerBuilderCanonicalName(),
              typeParameters,
              partitionedFields.headerSet.headerFields.size(),
              new HeaderIndexFn(partitionedFields, context)
      ));
    }

    return new ArrayList<>(List.of(builder.addParameters(parameters)
            .addCode(body.build())
            .build()));
  }

  @Override
  protected MethodSpec buildBind() {
    return transposition.builderBind(context);
  }

  private CodeBlock initialiseTypeVarField(RecognizerContext context, FieldModel fieldModel) {
    String fieldBuilderName = context.getFormatter().fieldBuilderName(fieldModel.getName().toString());

    return CodeBlock.builder().addStatement("this.$L = $L", fieldBuilderName, new TypeVarFieldInitializer(context, fieldModel)).build();
  }

  private CodeBlock initialiseParameterisedField(RecognizerContext context, FieldModel fieldModel) {
    String builderName = context.getFormatter().fieldBuilderName(fieldModel.getName().toString());
    return CodeBlock.of("this.$L = $L;\n", builderName, new FieldInitializer(fieldModel, true, context));
  }

  @Override
  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = super.buildFields();

    if (partitionedFields.hasHeaderFields()) {
      fieldSpecs.add(0, buildHeaderField());
    }

    return fieldSpecs;
  }

  private FieldSpec buildHeaderField() {
    RecognizerNameFormatter formatter = context.getFormatter();
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    HashSet<TypeMirror> headerTypeParameters = partitionedFields.headerSet.typeParameters();
    TypeName targetType = ClassName.bestGuess(formatter.headerClassName());

    if (!headerTypeParameters.isEmpty()) {
      targetType = ParameterizedTypeName.get((ClassName) targetType, headerTypeParameters.stream().map(TypeName::get).collect(Collectors.toList()).toArray(TypeName[]::new));
    }

    ClassName recognizingBuilder = ClassName.get(elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS));
    ParameterizedTypeName typedBuilder = ParameterizedTypeName.get(recognizingBuilder, targetType);

    return FieldSpec.builder(
            typedBuilder,
            formatter.headerBuilderFieldName(),
            Modifier.PRIVATE
    ).build();
  }

  @Override
  protected Emitter buildFeedIndexedBlock() {
    return new FeedIndexedEmitter(partitionedFields, context);
  }

  @Override
  protected Emitter buildResetBlock() {
    return new ResetEmitter(partitionedFields, context);
  }

  @Override
  protected List<FieldModel> getFields() {
    return partitionedFields.discriminate()
            .stream()
            .filter(f -> !f.isHeader())
            .flatMap(f -> {
              FieldDiscriminate.SingleField singleField = (FieldDiscriminate.SingleField) f;
              return Stream.of(singleField.getField());
            })
            .collect(Collectors.toList());
  }
}
