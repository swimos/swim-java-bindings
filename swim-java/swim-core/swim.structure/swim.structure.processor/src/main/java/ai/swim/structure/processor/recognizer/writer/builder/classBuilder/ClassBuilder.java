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

package ai.swim.structure.processor.recognizer.writer.builder.classBuilder;

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.builder.Builder;
import ai.swim.structure.processor.recognizer.writer.builder.FieldInitializer;
import ai.swim.structure.processor.recognizer.writer.builder.header.HeaderIndexFn;
import ai.swim.structure.processor.recognizer.writer.recognizer.Recognizer;
import ai.swim.structure.processor.recognizer.writer.recognizer.TypeVarFieldInitializer;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

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

import static ai.swim.structure.processor.recognizer.writer.Lookups.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.recognizer.writer.WriterUtils.typeParametersToTypeVariable;
import static ai.swim.structure.processor.recognizer.writer.WriterUtils.writeGenericRecognizerConstructor;

public class ClassBuilder extends Builder {

  private final Recognizer.Transposition transposition;

  public ClassBuilder(ClassSchema classSchema, ScopedContext context, Recognizer.Transposition transposition) {
    super(classSchema, context);
    this.transposition = transposition;
  }

  @Override
  protected TypeSpec.Builder init() {
    return TypeSpec.classBuilder(context.getNameFactory().builderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addTypeVariables(typeParametersToTypeVariable(schema.getTypeParameters()));
  }

  @Override
  protected List<MethodSpec> buildConstructors() {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    List<ParameterSpec> parameters = writeGenericRecognizerConstructor(schema.getTypeParameters(), context);
    CodeBlock.Builder body = CodeBlock.builder();

    PartitionedFields partitionedFields = schema.getPartitionedFields();


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
      NameFactory nameFactory = context.getNameFactory();
      HashSet<TypeMirror> headerTypeParameters = partitionedFields.headerSet.typeParameters();
      String typeParameters = headerTypeParameters.stream().map(ty -> nameFactory.typeParameterName(ty.toString())).collect(Collectors.joining(", "));

      body.add(CodeBlock.of(
          "this.$L = $L($L, () -> new $L($L), $L, $L);",
          nameFactory.headerBuilderFieldName(),
          nameFactory.headerBuilderMethod(),
          partitionedFields.headerSet.hasTagBody(),
          nameFactory.headerBuilderCanonicalName(),
          typeParameters,
          partitionedFields.headerSet.headerFields.size(),
          new HeaderIndexFn(schema).emit(context)
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

  private CodeBlock initialiseTypeVarField(ScopedContext context, FieldModel fieldModel) {
    NameFactory nameFactory = context.getNameFactory();
    String fieldBuilderName = nameFactory.fieldBuilderName(fieldModel.getName().toString());

    return CodeBlock.builder().addStatement("this.$L = $L", fieldBuilderName, new TypeVarFieldInitializer(fieldModel).emit(context).toString()).build();
  }

  private CodeBlock initialiseParameterisedField(ScopedContext context, FieldModel fieldModel) {
    NameFactory nameFactory = context.getNameFactory();
    String builderName = nameFactory.fieldBuilderName(fieldModel.getName().toString());

    return CodeBlock.of("this.$L = $L;\n", builderName, new FieldInitializer(fieldModel, true, false).emit(context));
  }

  @Override
  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = super.buildFields();

    if (schema.getPartitionedFields().hasHeaderFields()) {
      fieldSpecs.add(0, buildHeaderField());
    }

    return fieldSpecs;
  }

  private FieldSpec buildHeaderField() {
    NameFactory nameFactory = context.getNameFactory();
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    HashSet<TypeMirror> headerTypeParameters = partitionedFields.headerSet.typeParameters();

    TypeName targetType = ClassName.bestGuess(nameFactory.headerClassName());

    if (!headerTypeParameters.isEmpty()) {
      targetType = ParameterizedTypeName.get((ClassName) targetType, headerTypeParameters.stream().map(TypeName::get).collect(Collectors.toList()).toArray(TypeName[]::new));
    }

    ClassName recognizingBuilder = ClassName.get(elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS));
    ParameterizedTypeName typedBuilder = ParameterizedTypeName.get(recognizingBuilder, targetType);

    return FieldSpec.builder(
        typedBuilder,
        nameFactory.headerBuilderFieldName(),
        Modifier.PRIVATE
    ).build();
  }

  @Override
  protected Emitter buildFeedIndexedBlock() {
    return new FeedIndexedEmitter(schema);
  }

  @Override
  protected Emitter buildResetBlock() {
    return new ResetEmitter(schema);
  }

  @Override
  protected List<FieldModel> getFields() {
    return schema.discriminate()
        .stream()
        .filter(f -> !f.isHeader())
        .flatMap(f -> {
          FieldDiscriminate.SingleField singleField = (FieldDiscriminate.SingleField) f;
          return Stream.of(singleField.getField());
        })
        .collect(Collectors.toList());
  }
}
