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
import ai.swim.structure.processor.recognizer.writer.builder.header.HeaderIndexFn;
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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.swim.structure.processor.recognizer.writer.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.recognizer.writer.Lookups.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.recognizer.writer.WriterUtils.typeParametersToTypeVariable;
import static ai.swim.structure.processor.recognizer.writer.WriterUtils.writeGenericRecognizerConstructor;

public class ClassBuilder extends Builder {

  public ClassBuilder(ClassSchema classSchema, ScopedContext context) {
    super(classSchema, context);
  }

  @Override
  protected TypeSpec.Builder init() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(context.getNameFactory().builderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addMethod(buildDefaultConstructor())
        .addTypeVariables(typeParametersToTypeVariable(schema.getTypeParameters()));

    if (!schema.getTypeParameters().isEmpty()) {
      builder.addMethod(buildParameterisedConstructor());
    }

    return builder;
  }

  private MethodSpec buildDefaultConstructor() {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .build();
  }

  private MethodSpec buildParameterisedConstructor() {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    List<ParameterSpec> parameters = writeGenericRecognizerConstructor(schema.getTypeParameters(), context);
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel fieldModel : schema.getClassMap().getFieldModels()) {
      if (fieldModel.isParameterised(context)) {
        VariableElement element = fieldModel.getElement();
        TypeKind typeKind = element.asType().getKind();

        switch (typeKind) {
          case TYPEVAR:
            body.add(initialiseTypeVarField(context, fieldModel));
            break;
          case DECLARED:
            body.add(initialiseParameterisedField(context, fieldModel));
            break;
          default:
            throw new AssertionError("Unexpected type kind when processing generic parameters: " + typeKind + " in " + context.getRoot());
        }
      }
    }

    return builder.addParameters(parameters)
        .addCode(body.build())
        .build();
  }

  private CodeBlock initialiseTypeVarField(ScopedContext context, FieldModel fieldModel) {
    NameFactory nameFactory = context.getNameFactory();
    String fieldBuilderName = nameFactory.fieldBuilderName(fieldModel.getName().toString());

    return CodeBlock.builder().addStatement("this.$L = $L", fieldBuilderName, new TypeVarFieldInitializer(fieldModel).emit(context).toString()).build();
  }

  private CodeBlock initialiseParameterisedField(ScopedContext context, FieldModel fieldModel) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);
    DeclaredType typedBuilder = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldModel.type(processingEnvironment));

    NameFactory nameFactory = context.getNameFactory();
    String builderName = nameFactory.fieldBuilderName(fieldModel.getName().toString());

    return CodeBlock.of("this.$L = new $T($L);\n", builderName, typedBuilder, fieldModel.initializer(context, true, false));
  }

  @Override
  protected Emitter buildBindBlock() {
    return new BindEmitter(schema);
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
    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);

    TypeName targetType = ClassName.get(recognizingBuilderElement);
    if (!headerTypeParameters.isEmpty()) {
      targetType = ParameterizedTypeName.get((ClassName) targetType, headerTypeParameters.stream().map(TypeName::get).collect(Collectors.toList()).toArray(TypeName[]::new));
    }

    FieldSpec.Builder fieldBuilder = FieldSpec.builder(
        targetType,
        nameFactory.headerBuilderFieldName(),
        Modifier.PRIVATE
    );

    int numSlots = partitionedFields.headerSet.headerFields.size();

    boolean hasBody = partitionedFields.headerSet.hasTagBody();
    String headerBuilder = nameFactory.headerBuilderCanonicalName();

    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add(
        "$L($L, () -> new $L(), $L, $L)",
        nameFactory.headerBuilderMethod(),
        hasBody,
        headerBuilder,
        numSlots,
        new HeaderIndexFn(schema).emit(context)
    );

    return fieldBuilder.initializer(initializer.build()).build();
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
