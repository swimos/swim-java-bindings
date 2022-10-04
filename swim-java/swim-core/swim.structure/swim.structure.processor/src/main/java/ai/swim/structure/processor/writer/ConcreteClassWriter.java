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

package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

import static ai.swim.structure.processor.writer.Lookups.HEADER_NO_SLOTS;
import static ai.swim.structure.processor.writer.Lookups.HEADER_WRITER;

public class ConcreteClassWriter extends ClassWriter {

  public ConcreteClassWriter(ScopedContext scopedContext, ClassSchema classSchema) {
    super(scopedContext, classSchema);
  }

  @Override
  public CodeBlock writeIntoBody() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();

    Elements elementUtils = processingEnvironment.getElementUtils();
    TypeElement stringTypeElement = elementUtils.getTypeElement(String.class.getCanonicalName());
    Model stringWriter = context.getWriterFactory().lookup(stringTypeElement.asType());

    NameFactory nameFactory = context.getNameFactory();

    PartitionedFields partitionedFields = classSchema.getPartitionedFields();
    HeaderSet headerSet = partitionedFields.headerSet;
    List<FieldModel> attributes = headerSet.attributes;

    CodeBlock.Builder body = CodeBlock.builder();

    body.addStatement("int numAttrs = $L", attributes.size());

    TypeElement rawHeaderWriter = elementUtils.getTypeElement(HEADER_WRITER);
    ParameterizedTypeName headerWriter = ParameterizedTypeName.get(ClassName.get(rawHeaderWriter), writerType);

    body.addStatement("$T recWriter = structuralWriter.record(numAttrs)", headerWriter);

    if (headerSet.headerFields.isEmpty()) {
      if (headerSet.tagBody == null) {
        body.addStatement("recWriter = recWriter.writeExtantAttr(\"$L\")", nameFactory.getName());
      } else {
        body.addStatement("recWriter = recWriter.writeAttrWith(\"$L\", $L)", nameFactory.getName(), stringWriter.initializer(context, false, false));
      }
    } else {
      body.addStatement("recWriter = recWriter.writeAttrWith(\"$L\", $L)", nameFactory.getName(), makeHeader());
    }

    return body.build();
  }

  private CodeBlock makeHeader() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement noSlots = elementUtils.getTypeElement(HEADER_NO_SLOTS);
    System.out.println("Noslots: " + noSlots);
    PartitionedFields partitionedFields = classSchema.getPartitionedFields();
    List<FieldModel> headerFields = partitionedFields.headerSet.headerFields;
    FieldModel tagBody = partitionedFields.headerSet.tagBody;

    CodeBlock.Builder headerBlock = CodeBlock.builder();
    headerBlock.add("$L", noSlots);

    for (int i = headerFields.size() - 1; i >= 0; i--) {
      FieldModel fieldModel = headerFields.get(i);
      CodeBlock.Builder getter = CodeBlock.builder();
      fieldModel.getAccessor().writeGet(getter, "from");
      headerBlock.add(".prepend($L, $L)", fieldModel.propertyName(), getter.build());
    }

    if (tagBody != null) {
      CodeBlock.Builder getter = CodeBlock.builder();
      tagBody.getAccessor().writeGet(getter, "from");
      headerBlock.add(".withBody($L, $L)", tagBody.propertyName(), getter.build());
    }

    return headerBlock.build();
  }


}
