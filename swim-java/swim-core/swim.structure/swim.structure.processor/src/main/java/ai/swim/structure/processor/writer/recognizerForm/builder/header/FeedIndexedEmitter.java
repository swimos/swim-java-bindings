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

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

public class FeedIndexedEmitter extends Emitter {
  private final List<FieldModel> fields;
  private final RecognizerContext context;

  public FeedIndexedEmitter(List<FieldModel> fields, RecognizerContext context) {
    this.fields = fields;
    this.context = context;
  }

  @Override
  public String toString() {
    CodeBlock.Builder body = CodeBlock.builder();

    body.beginControlFlow("switch (index)");

    for (int i = 0; i < this.fields.size(); i++) {
      FieldModel field = fields.get(i);
      String fieldName = context.getFormatter().fieldBuilderName(field.getName().toString());

      body.add("case $L:", i);
      body.addStatement("\nreturn this.$L.feed(event)", fieldName);
    }

    body.add("default:").addStatement("\nthrow new RuntimeException(\"Unknown idx: \" + index)").endControlFlow();

    return body.build().toString();
  }
}
