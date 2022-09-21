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

import ai.swim.structure.processor.recognizer.context.NameFactory;
import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.recognizer.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

public class ResetEmitter implements Emitter {
  private final ClassSchema schema;

  public ResetEmitter(ClassSchema schema) {
    this.schema = schema;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();
    NameFactory nameFactory = context.getNameFactory();

    for (FieldDiscriminate field : schema.discriminate()) {
      String fieldName;
      if (field.isHeader()) {
        fieldName = nameFactory.headerBuilderFieldName();
      } else {
        FieldDiscriminate.SingleField fieldDiscriminate = (FieldDiscriminate.SingleField) field;
        fieldName = context.getNameFactory().fieldBuilderName(fieldDiscriminate.getField().getName().toString());
      }

      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");
    return body.build();
  }
}
