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

import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerNameFormatter;
import com.squareup.javapoet.CodeBlock;

/**
 * Emitter for building the recognizer's reset method:
 * <pre>
 *   {@code
 *     @Override
 *     public RecognizingBuilder<ProcessorTest.OptionalFieldClass> reset() {
 *       this.bBuilder = this.bBuilder.reset();
 *       this.aBuilder = this.aBuilder.reset();
 *       return this;
 *     }
 *   }
 * </pre>
 */
public class ResetEmitter extends Emitter {
  private final PartitionedFields fields;
  private final RecognizerContext context;

  public ResetEmitter(PartitionedFields fields, RecognizerContext context) {
    this.fields = fields;
    this.context = context;
  }

  @Override
  public String toString() {
    CodeBlock.Builder body = CodeBlock.builder();
    RecognizerNameFormatter formatter = context.getFormatter();

    for (FieldDiscriminate field : fields.discriminate()) {
      String fieldName;
      if (field.isHeader()) {
        fieldName = formatter.headerBuilderFieldName();
      } else {
        FieldDiscriminate.SingleField fieldDiscriminate = (FieldDiscriminate.SingleField) field;
        fieldName = formatter.fieldBuilderName(fieldDiscriminate.getField().getName().toString());
      }

      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");
    return body.build().toString();
  }
}
