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

package ai.swim.structure.processor.writer.recognizerForm.builder.header;

import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.schema.HeaderSpec;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.WriterUtils;
import ai.swim.structure.processor.writer.recognizerForm.Lookups;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import com.squareup.javapoet.CodeBlock;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Header index method functional interface emitter:
 *
 * <pre>
 *   {@code
 *       if (key.isHeaderBody()) {
 *         return 0;
 *       }
 *       if (key.isHeaderSlot()) {
 *         HeaderSlotKey headerSlotKey = (HeaderSlotKey) key;
 *         switch (headerSlotKey.getName()) {
 *           case "value":
 *           	 return 1;
 *           default:	throw new RuntimeException("Unexpected key: " + key);
 *         }
 *       }
 *       return null;
 *       });
 *   }
 * </pre>
 */
public class HeaderIndexFn extends Emitter {
  private final PartitionedFields fields;
  private final RecognizerContext context;

  public HeaderIndexFn(PartitionedFields fields, RecognizerContext context) {
    this.fields = fields;
    this.context = context;
  }

  @Override
  public String toString() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    HeaderSpec headerFieldSet = fields.headerSpec;

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

    return body.build().toString();
  }
}
