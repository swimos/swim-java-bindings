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

import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

/**
 * Header reset emitter:
 * <pre>
 *   {@code
 *    this.laneBuilder = this.laneBuilder.reset();
 *    return this;
 *   }
 * </pre>
 */
public class ResetEmitter extends Emitter {
  private final List<FieldModel> fields;
  private final RecognizerContext context;

  public ResetEmitter(List<FieldModel> fields, RecognizerContext context) {
    this.fields = fields;
    this.context = context;
  }


  @Override
  public String toString() {
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel field : this.fields) {
      String fieldName = context.getFormatter().fieldBuilderName(field.getName().toString());
      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");

    return body.build().toString();
  }
}
