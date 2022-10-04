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

package ai.swim.structure.processor.recognizer.writer.builder.header;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.Emitter;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

public class FeedIndexedEmitther implements Emitter {
  private final List<FieldModel> fields;

  public FeedIndexedEmitther(List<FieldModel> fields) {
    this.fields = fields;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();

    body.beginControlFlow("switch (index)");

    for (int i = 0; i < this.fields.size(); i++) {
      FieldModel field = fields.get(i);
      String fieldName = context.getNameFactory().fieldBuilderName(field.getName().toString());

      body.add("case $L:", i);
      body.addStatement("\nreturn this.$L.feed(event)", fieldName);
    }

    body.add("default:").addStatement("\nthrow new RuntimeException(\"Unknown idx: \" + index)").endControlFlow();

    return body.build();
  }
}
