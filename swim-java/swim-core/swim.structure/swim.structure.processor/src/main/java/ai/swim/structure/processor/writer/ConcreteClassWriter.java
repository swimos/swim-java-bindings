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

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.RuntimeLookupModel;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

import static ai.swim.structure.processor.writer.Lookups.BODY_WRITER;
import static ai.swim.structure.processor.writer.Lookups.HEADER_NO_SLOTS;
import static ai.swim.structure.processor.writer.Lookups.HEADER_WRITER;

public class ConcreteClassWriter extends ClassWriter {

  private final ClassSchema classSchema;

  public ConcreteClassWriter(ScopedContext scopedContext, ClassSchema classSchema) {
    super(scopedContext);
    this.classSchema = classSchema;
  }

  @Override
  public Iterable<FieldSpec> getFields() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();
    TypeElement writableElement = elementUtils.getTypeElement(Lookups.WRITABLE_CLASS);
    TypeElement classElement = elementUtils.getTypeElement(Class.class.getCanonicalName());

    List<FieldSpec> fields = new ArrayList<>();

    for (FieldModel fieldModel : classSchema.getClassMap().getFieldModels()) {
      if (fieldModel.getModel() instanceof RuntimeLookupModel) {
        String writableName = nameFactory.writableName(fieldModel.propertyName());
        TypeMirror fieldType = fieldModel.type(processingEnvironment);
        DeclaredType writableType = typeUtils.getDeclaredType(writableElement, fieldType);

        FieldSpec writerField = FieldSpec
            .builder(TypeName.get(writableType), writableName)
            .addModifiers(Modifier.PRIVATE)
            .build();
        fields.add(writerField);

        DeclaredType classType = typeUtils.getDeclaredType(classElement, fieldType);
        FieldSpec classField = FieldSpec
            .builder(TypeName.get(classType), String.format("%sClass", writableName))
            .addModifiers(Modifier.PRIVATE)
            .build();
        fields.add(classField);
      }
    }

    return fields;
  }

  public CodeBlock.Builder buildInit() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    CodeBlock.Builder body = CodeBlock.builder();

    if (!classSchema.getPartitionedFields().body.isReplaced()) {
      body.add("int __numSlots = 0;\n");
    }

    // unpack the object and for any fields that are not null and require a runtime lookup, look them up and store them.
    for (FieldModel fieldModel : classSchema.getClassMap().getFieldModels()) {
      if (fieldModel.isIgnored()) {
        continue;
      }

      String fieldName = fieldModel.propertyName();
      TypeMirror rawType = fieldModel.getElement().asType();

      CodeBlock.Builder getter = CodeBlock.builder();
      fieldModel.getAccessor().writeGet(getter, "from");

      // Check the element's type to avoid boxing.
      if (rawType.getKind().isPrimitive()) {
        body.addStatement("$T $L = $L", rawType, fieldName, getter.build());
      } else {
        // As the field may have been unrolled, we need to cast it to the right type so its writer can operate on the
        // unrolled type and coerce it to the element's type.
        TypeMirror type = fieldModel.type(processingEnvironment);
        body.addStatement("$T $L = ($T) $L", type, fieldName, type, getter.build());
      }

      if (fieldModel.getModel() instanceof RuntimeLookupModel) {
        String writableName = nameFactory.writableName(fieldModel.propertyName());
        body.beginControlFlow("if ($L != null)", fieldName);

        if (!classSchema.getPartitionedFields().body.isReplaced()) {
          body.addStatement("__numSlots += 1");
        }

        TypeElement classElement = elementUtils.getTypeElement(Class.class.getCanonicalName());
        DeclaredType classType = typeUtils.getDeclaredType(classElement, fieldModel.type(processingEnvironment));

        body.beginControlFlow("if ($L == null || $LClass != $L.getClass())", writableName, writableName, fieldName)
            .addStatement("$L = getProxy().lookupObject($L)", writableName, fieldName)
            .addStatement("$LClass = ($T) $L.getClass()", writableName, TypeName.get(classType), fieldName)
            .endControlFlow()
            .endControlFlow();
      }
    }

    return body;
  }

  private CodeBlock writeAttrs() {
    PartitionedFields partitionedFields = classSchema.getPartitionedFields();
    HeaderSet headerSet = partitionedFields.headerSet;
    CodeBlock.Builder body = CodeBlock.builder();

    if (classSchema.getClassMap().isEnum()) {
      body.addStatement("String __tag");
      body.beginControlFlow("switch (from)");

      Element root = context.getRoot();
      for (Element enclosedElement : root.getEnclosedElements()) {
        if (enclosedElement.getKind().equals(ElementKind.ENUM_CONSTANT)) {
          String tagString = enclosedElement.toString();
          AutoForm.Tag tag = enclosedElement.getAnnotation(AutoForm.Tag.class);

          if (tag != null && !tag.value().isBlank()) {
            tagString = tag.value();
          }

          body
              .add("$> case $L:", enclosedElement.toString())
              .addStatement("$> __tag = \"$L\"", tagString)
              .addStatement("break")
              .add("$<$<");
        }
      }

      body
          .add("$>default:")
          .addStatement("$> throw new AssertionError(\"Bug: Unhandled enum constant during annotation processing for: $L\")", root)
          .add("$<$<")
          .endControlFlow();
    } else {
      body.addStatement("String __tag = \"$L\"", classSchema.getTag());
    }

    if (headerSet.headerFields.isEmpty()) {
      FieldModel tagBody = headerSet.tagBody;
      if (tagBody == null) {
        body.addStatement("__recWriter = __recWriter.writeExtantAttr(__tag)");
      } else {
        CodeBlock.Builder getter = CodeBlock.builder();
        tagBody.getAccessor().writeGet(getter, "from");
        body.addStatement("__recWriter = __recWriter.writeAttr(__tag, $L, $L)", tagBody.initializer(context, false, false), getter.build());
      }
    } else {
      body.addStatement("__recWriter = __recWriter.writeAttr(__tag, $L\n)", makeHeader());
    }

    headerSet.attributes.forEach(attr -> {
      CodeBlock.Builder getter = CodeBlock.builder();
      attr.getAccessor().writeGet(getter, "from");
      body.addStatement("__recWriter = __recWriter.writeAttr($S, $L, $L)", attr.propertyName(), attr.initializer(context, false, false), getter.build());
    });

    return body.build();
  }

  private CodeBlock writeSlots() {
    PartitionedFields partitionedFields = classSchema.getPartitionedFields();
    NameFactory nameFactory = context.getNameFactory();
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    CodeBlock.Builder body = CodeBlock.builder();

    if (partitionedFields.body.isReplaced()) {
      FieldModel formBody = partitionedFields.body.getFields().get(0);
      CodeBlock.Builder getter = CodeBlock.builder();
      formBody.getAccessor().writeGet(getter, "from");

      body.addStatement("return __recWriter.delegate($L, $L)", formBody.initializer(context, false, false), getter.build());
    } else {
      CodeBlock.Builder bodyStatements = CodeBlock.builder();

      TypeElement rawBodyWriter = elementUtils.getTypeElement(BODY_WRITER);
      ParameterizedTypeName bodyWriter = ParameterizedTypeName.get(ClassName.get(rawBodyWriter), writerType);

      bodyStatements.add("\r\n$T __bodyWriter = __recWriter.completeHeader(__numSlots);\n\r", bodyWriter);

      for (FieldModel bodyField : partitionedFields.body.getFields()) {
        boolean isRuntimeLookup = bodyField.getModel() instanceof RuntimeLookupModel;
        String fieldName = bodyField.propertyName();
        TypeMirror rawType = bodyField.getElement().asType();

        CodeBlock init = isRuntimeLookup ? CodeBlock.of("$L", nameFactory.writableName(bodyField.propertyName())) : bodyField.initializer(context, false, false);

        CodeBlock writeOp = CodeBlock
            .builder()
            .addStatement("__bodyWriter = __bodyWriter.writeSlot($S, $L, $L)", fieldName, init, fieldName)
            .build();

        if (!rawType.getKind().isPrimitive()) {
          bodyStatements
              .beginControlFlow("if ($L != null)", fieldName)
              .add(writeOp)
              .endControlFlow();
        } else {
          bodyStatements.add(writeOp);
        }
      }

      body.add(bodyStatements.addStatement("return __bodyWriter.done()").build());
    }

    return body.build();
  }

  private CodeBlock makeHeader() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    TypeElement noSlots = elementUtils.getTypeElement(HEADER_NO_SLOTS);
    PartitionedFields partitionedFields = classSchema.getPartitionedFields();
    List<FieldModel> headerFields = partitionedFields.headerSet.headerFields;
    FieldModel tagBody = partitionedFields.headerSet.tagBody;

    CodeBlock.Builder headerBlock = CodeBlock.builder();
    headerBlock.add("$L", noSlots);

    for (int i = headerFields.size() - 1; i >= 0; i--) {
      FieldModel fieldModel = headerFields.get(i);
      boolean isRuntimeLookup = fieldModel.getModel().isRuntimeLookup();

      CodeBlock.Builder getter = CodeBlock.builder();
      fieldModel.getAccessor().writeGet(getter, "from");
      headerBlock.add(
          "\n.prepend($S, $L, $L)",
          fieldModel.propertyName(),
          isRuntimeLookup ? nameFactory.writableName(fieldModel.propertyName()) : fieldModel.initializer(context, false, false),
          getter.build());
    }

    if (tagBody == null) {
      headerBlock.add("\n.simple()");
    } else {
      CodeBlock.Builder getter = CodeBlock.builder();
      tagBody.getAccessor().writeGet(getter, "from");
      headerBlock.add("\n.withBody($L, $L)", tagBody.initializer(context, false, false), getter.build());
    }

    return headerBlock.build();
  }

  @Override
  public CodeBlock writeIntoBody() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement rawHeaderWriter = elementUtils.getTypeElement(HEADER_WRITER);
    ParameterizedTypeName headerWriter = ParameterizedTypeName.get(ClassName.get(rawHeaderWriter), writerType);

    return buildInit()
        .addStatement("int __numAttrs = $L", classSchema.getPartitionedFields().headerSet.attributes.size())
        .addStatement("$T __recWriter = structuralWriter.record(__numAttrs)", headerWriter)
        .add(writeAttrs())
        .add(writeSlots())
        .build();
  }

}
