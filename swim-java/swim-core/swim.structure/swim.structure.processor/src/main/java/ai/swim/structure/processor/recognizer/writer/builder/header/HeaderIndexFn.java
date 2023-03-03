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

import ai.swim.structure.processor.Emitter;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.writer.Lookups;
import ai.swim.structure.processor.recognizer.writer.WriterUtils;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class HeaderIndexFn implements Emitter {
  private final ClassSchema schema;

  public HeaderIndexFn(ClassSchema schema) {
    this.schema = schema;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    HeaderSet headerFieldSet = partitionedFields.headerSet;

    int idx = 0;
    CodeBlock.Builder body = CodeBlock.builder();
    body.add("(key) -> {\n");

    if (headerFieldSet.hasTagBody()) {
      body.beginControlFlow("if (key.isHeaderBody())");
      body.addStatement("return $L", idx);
      body.endControlFlow();

      idx += 1;
    }

    TypeElement headerSlotKey = elementUtils.getTypeElement(Lookups.DELEGATE_HEADER_SLOT_KEY);

    body.beginControlFlow("if (key.isHeaderSlot())");
    body.addStatement("$T headerSlotKey = ($T) key", headerSlotKey, headerSlotKey);

    WriterUtils.writeIndexSwitchBlock(
        body,
        "headerSlotKey.getName()",
        idx,
        (offset, i) -> {
          if (i - offset == headerFieldSet.headerFields.size()) {
            return null;
          } else {
            FieldModel recognizer = headerFieldSet.headerFields.get(i - offset);
            return String.format("case \"%s\":\r\n\t return %s;\r\n", recognizer.propertyName(), i);
          }
        }
    );

    body.endControlFlow();
    body.addStatement("return null");
    body.add("}");

    return body.build();
  }
}
