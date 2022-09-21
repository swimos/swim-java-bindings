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

import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.Emitter;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.TypeMirror;

public class BindEmitter implements Emitter {
  private final ClassSchema schema;

  public BindEmitter(ClassSchema schema) {
    this.schema = schema;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();
    TypeMirror ty = context.getRoot().asType();
    body.add("$T obj = new $T();\n\n", ty, ty);

    for (FieldDiscriminate fieldDiscriminate : schema.discriminate()) {
      if (fieldDiscriminate.isHeader()) {
        FieldDiscriminate.HeaderFields headerFields = (FieldDiscriminate.HeaderFields) fieldDiscriminate;
        ClassName headerElement = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());

        body.addStatement("$T header = this.headerBuilder.bind()", headerElement);

        for (FieldModel field : headerFields.getFields()) {
          field.getAccessor().write(body, "obj", String.format("header.%s", field.getName().toString()));
        }

        FieldModel tagBody = headerFields.getTagBody();

        if (tagBody != null) {
          tagBody.getAccessor().write(body, "obj", String.format("header.%s", tagBody.getName().toString()));
        }
      } else {
        FieldDiscriminate.SingleField singleField = (FieldDiscriminate.SingleField) fieldDiscriminate;
        FieldModel field = singleField.getField();
        String fieldName = context.getNameFactory().fieldBuilderName(field.getName().toString());

        if (field.isOptional()) {
          field.getAccessor().write(body, "obj", String.format("this.%s.bindOr(%s)", fieldName, field.defaultValue()));
        } else {
          field.getAccessor().write(body, "obj", String.format("this.%s.bind()", fieldName));
        }
      }
    }

    body.add("\nreturn obj;");
    return body.build();
  }
}
