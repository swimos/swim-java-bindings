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

package ai.swim.structure.processor.writer.recognizerForm.builder.classBuilder;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.schema.FieldDiscriminant;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerNameFormatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import static ai.swim.structure.processor.writer.recognizerForm.builder.BuilderWriter.typeParametersToVariables;

/**
 * Recognizer bind method emitter.
 * <p>
 * Builds a bind block like:
 * <pre>
 * {@code
 *     @Override
 *     public OptionalFieldClass bind() {
 *       OptionalFieldClass obj = new OptionalFieldClass();
 *
 *       obj.b = this.bBuilder.bind();
 *       obj.setA(this.aBuilder.bindOr(0));
 *
 *       return obj;
 *     }
 * }
 * </pre>
 */
public class BindEmitter extends Emitter {
  private final ClassLikeModel model;
  private final PartitionedFields fields;
  private final RecognizerContext context;

  public BindEmitter(ClassLikeModel model, PartitionedFields fields, RecognizerContext context) {
    this.model = model;
    this.fields = fields;
    this.context = context;
  }

  @Override
  public String toString() {
    CodeBlock.Builder body = CodeBlock.builder();
    RecognizerNameFormatter formatter = context.getFormatter();
    TypeMirror ty = context.getRoot().asType();
    // Instantiate the target class
    body.add("$T obj = new $T();\n\n", ty, ty);

    for (FieldDiscriminant fieldDiscriminate : fields.discriminate()) {
      if (fieldDiscriminate.isHeader()) {
        FieldDiscriminant.HeaderFields headerFields = (FieldDiscriminant.HeaderFields) fieldDiscriminate;

        List<TypeVariableName> mappedTypeParameters = typeParametersToVariables(
            fields.headerSpec.typeParameters(),
            model.getTypeParameters(),
            context.getRoot());
        ClassName headerElement = ClassName.bestGuess(formatter.headerCanonicalName());

        if (mappedTypeParameters.isEmpty()) {
          body.addStatement("$T header = this.headerBuilder.bind()", headerElement);
        } else {
          body.addStatement(
              "$T header = this.headerBuilder.bind()",
              ParameterizedTypeName.get(headerElement, mappedTypeParameters.toArray(TypeName[]::new)));
        }

        for (FieldModel field : headerFields.getFields()) {
          field.getAccessor().writeSet(body, "obj", String.format("header.%s", field.getName().toString()));
        }

        FieldModel tagBody = headerFields.getTagBody();

        if (tagBody != null) {
          tagBody.getAccessor().writeSet(body, "obj", String.format("header.%s", tagBody.getName().toString()));
        }
      } else {
        FieldDiscriminant.SingleField singleField = (FieldDiscriminant.SingleField) fieldDiscriminate;
        FieldModel field = singleField.getField();
        String fieldName = formatter.fieldBuilderName(field.getName().toString());

        if (field.isOptional()) {
          field
              .getAccessor()
              .writeSet(body, "obj", String.format("this.%s.bindOr(%s)", fieldName, field.defaultValue()));
        } else {
          field.getAccessor().writeSet(body, "obj", String.format("this.%s.bind()", fieldName));
        }
      }
    }

    body.add("\nreturn obj;");
    return body.build().toString();
  }
}
