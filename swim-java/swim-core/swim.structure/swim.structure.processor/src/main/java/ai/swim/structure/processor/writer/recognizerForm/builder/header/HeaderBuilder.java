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

package ai.swim.structure.processor.writer.recognizerForm.builder.header;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizerForm.Lookups;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerNameFormatter;
import ai.swim.structure.processor.writer.recognizerForm.builder.Builder;
import ai.swim.structure.processor.writer.recognizerForm.builder.FieldInitializer;
import com.squareup.javapoet.*;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.recognizerForm.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

/**
 * Header builder and recognizer for classes that have fields promoted to headers. This emits a nested class for the
 * parent recognizer.
 */
public class HeaderBuilder extends Builder {
  private final List<TypeVariableName> typeParameters;

  public HeaderBuilder(ClassLikeModel model, PartitionedFields partitionedFields, RecognizerContext context, List<TypeVariableName> typeParameters) {
    super(model, partitionedFields, context);
    this.typeParameters = typeParameters;
  }

  @Override
  protected TypeSpec.Builder init() {
    return TypeSpec.classBuilder(context.getFormatter().headerBuilderClassName())
      .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
      .addTypeVariables(typeParameters);
  }

  @Override
  protected List<MethodSpec> buildConstructors() {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder()
      .addModifiers(Modifier.PUBLIC);

    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();
    RecognizerNameFormatter formatter = context.getFormatter();

    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);
    TypeElement recognizerTypeElement = elementUtils.getTypeElement(Lookups.TYPE_PARAMETER);

    for (FieldModel field : fields) {
      String builderName = formatter.fieldBuilderName(field.getName().toString());

      if (field.isParameterised()) {
        String fieldName = formatter.typeParameterName(field.getName().toString());
        TypeMirror fieldType = field.type();
        DeclaredType recognizerType = typeUtils.getDeclaredType(recognizerTypeElement, fieldType);
        DeclaredType builderType = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldType);

        builder.addParameter(ParameterSpec.builder(TypeName.get(recognizerType), fieldName).build());
        builder.addStatement("this.$L = new $T($L.build())", builderName, builderType, fieldName);
      } else {
        builder.addStatement("this.$L = $L", builderName, new FieldInitializer(field, true, context));
      }
    }

    return new ArrayList<>(List.of(builder.build()));
  }

  @Override
  protected MethodSpec buildBind() {
    TypeName classType = ClassName.bestGuess(context.getFormatter().headerCanonicalName());

    if (!typeParameters.isEmpty()) {
      classType = ParameterizedTypeName.get((ClassName) classType, typeParameters.toArray(TypeName[]::new));
    }

    MethodSpec.Builder builder = MethodSpec.methodBuilder(Lookups.RECOGNIZING_BUILDER_BIND)
      .addModifiers(Modifier.PUBLIC)
      .addAnnotation(Override.class)
      .returns(classType);
    builder.addCode(new BindEmitter(fields, context).toString());

    return builder.build();
  }

  @Override
  protected Emitter buildFeedIndexedBlock() {
    return new FeedIndexedEmitter(fields, context);
  }

  @Override
  protected Emitter buildResetBlock() {
    return new ResetEmitter(fields, context);
  }

  @Override
  protected List<FieldModel> getFields() {
    return partitionedFields.discriminate()
      .stream()
      .filter(FieldDiscriminate::isHeader)
      .flatMap(f -> {
        FieldDiscriminate.HeaderFields headerFields = (FieldDiscriminate.HeaderFields) f;
        FieldModel tagBody = headerFields.getTagBody();

        if (tagBody != null) {
          List<FieldModel> fields = new ArrayList<>(Collections.singleton(headerFields.getTagBody()));
          fields.addAll(headerFields.getFields());
          return fields.stream();
        } else {
          return headerFields.getFields().stream();
        }
      })
      .collect(Collectors.toList());
  }
}
