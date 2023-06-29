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

package ai.swim.structure.processor.writer.writerForm;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.schema.HeaderSpec;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.*;

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

import static ai.swim.structure.processor.writer.writerForm.Lookups.*;

public class ConcreteClassWriter extends ClassWriter {

  private final ClassLikeModel model;
  private final PartitionedFields fields;

  public ConcreteClassWriter(TypeElement root, WriterContext context, ClassLikeModel model, PartitionedFields fields) {
    super(root, context);
    this.model = model;
    this.fields = fields;
  }

  @Override
  public Iterable<FieldSpec> getFields() {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();
    TypeElement writableElement = elementUtils.getTypeElement(Lookups.WRITABLE_CLASS);
    TypeElement classElement = elementUtils.getTypeElement(Class.class.getCanonicalName());

    List<FieldSpec> fields = new ArrayList<>();

    for (FieldModel fieldModel : model.getFields()) {
      String writableName = context.getFormatter().writableName(fieldModel.propertyName());

      if (fieldModel.getModel().isUnresolved()) {
        TypeMirror fieldType = fieldModel.type();
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
      } else {
        InitializedType initializedType = fieldModel.instantiate(context.getInitializer(), false);
        DeclaredType writableType = typeUtils.getDeclaredType(writableElement, initializedType.getMirror());
        FieldSpec classField = FieldSpec
            .builder(TypeName.get(writableType), writableName)
            .initializer(initializedType.getInitializer())
            .build();
        fields.add(classField);
      }
    }

    return fields;
  }

  public CodeBlock.Builder buildInit() {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();

    CodeBlock.Builder body = CodeBlock.builder();

    if (!fields.body.isReplaced()) {
      body.add("int __numSlots = 0;\n");
    }

    // unpack the object and for any fields that are not null and require a runtime lookup, look them up and store them.
    for (FieldModel fieldModel : model.getFields()) {
      if (fieldModel.isIgnored()) {
        continue;
      }

      String fieldName = fieldModel.propertyName();
      TypeMirror rawType = fieldModel.type();

      CodeBlock.Builder getter = CodeBlock.builder();
      fieldModel.getAccessor().writeGet(getter, "from");

      // Check the element's type to avoid boxing.
      if (rawType.getKind().isPrimitive()) {
        body.addStatement("$T $L = $L", rawType, fieldName, getter.build());
      } else {
        // As the field may have been unrolled, we need to cast it to the right type so its writer can operate on the
        // unrolled type and coerce it to the element's type.
        TypeMirror type = fieldModel.type();
        body.addStatement("$T $L = ($T) $L", type, fieldName, type, getter.build());
      }

      if (!rawType.getKind().isPrimitive()) {
        body.beginControlFlow("if ($L != null)", fieldName);

        if (!fields.body.isReplaced()) {
          body.addStatement("__numSlots += 1");
        }

<<<<<<<< HEAD:swim-java/swim-core/swim-structure/swim-structure-processor/src/main/java/ai/swim/structure/processor/writer/writerForm/ConcreteClassWriter.java
        if (fieldModel.getModel().isUnresolved()) {
          String writableName = context.getFormatter().writableName(fieldModel.propertyName());

          TypeElement classElement = elementUtils.getTypeElement(Class.class.getCanonicalName());
          DeclaredType classType = typeUtils.getDeclaredType(classElement, fieldModel.type());
========
        if (fieldModel.getModel() instanceof RuntimeLookupModel) {
          String writableName = nameFactory.writableName(fieldModel.propertyName());


          TypeElement classElement = elementUtils.getTypeElement(Class.class.getCanonicalName());
          DeclaredType classType = typeUtils.getDeclaredType(classElement, fieldModel.type(processingEnvironment));
>>>>>>>> 01c6f3e20f9238147e06d257e083e8a71267fd37:swim-java/swim-core/swim-structure/swim-structure-processor/src/main/java/ai/swim/structure/processor/writer/ConcreteClassWriter.java

          body.beginControlFlow("if ($L == null || $LClass != $L.getClass())", writableName, writableName, fieldName)
              .addStatement("$L = getProxy().lookupObject($L)", writableName, fieldName)
              .addStatement("$LClass = ($T) $L.getClass()", writableName, TypeName.get(classType), fieldName)
              .endControlFlow();
        }

        body.endControlFlow();
      }
    }

    return body;
  }

  private CodeBlock writeAttrs() {
    HeaderSpec headerSpec = fields.headerSpec;
    CodeBlock.Builder body = CodeBlock.builder();

    if (model.isEnum()) {
      body.addStatement("String __tag");
      body.beginControlFlow("switch (from)");

      Element root = model.getElement();
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
      body.addStatement("String __tag = \"$L\"", model.getTag());
    }

    if (headerSpec.headerFields.isEmpty()) {
      FieldModel tagBody = headerSpec.tagBody;
      if (tagBody == null) {
        body.addStatement("__recWriter = __recWriter.writeExtantAttr(__tag)");
      } else {
        CodeBlock.Builder getter = CodeBlock.builder();
        tagBody.getAccessor().writeGet(getter, "from");
        String writableName = context.getFormatter().writableName(tagBody.propertyName());
        body.addStatement("__recWriter = __recWriter.writeAttr(__tag, $L, $L)", writableName, getter.build());
      }
    } else {
      body.addStatement("__recWriter = __recWriter.writeAttr(__tag, $L\n)", makeHeader());
    }

    headerSpec.attributes.forEach(attr -> {
      CodeBlock.Builder getter = CodeBlock.builder();
      attr.getAccessor().writeGet(getter, "from");
      String writableName = context.getFormatter().writableName(attr.propertyName());
      body.addStatement("__recWriter = __recWriter.writeAttr($S, $L, $L)", attr.propertyName(), writableName, getter.build());
    });

    return body.build();
  }

  private CodeBlock writeSlots() {
    Elements elementUtils = context.getElementUtils();

    CodeBlock.Builder body = CodeBlock.builder();

    if (fields.body.isReplaced()) {
      FieldModel formBody = fields.body.getFields().get(0);
      CodeBlock.Builder getter = CodeBlock.builder();
      formBody.getAccessor().writeGet(getter, "from");
      String writableName = context.getFormatter().writableName(formBody.propertyName());

      body.addStatement("return __recWriter.delegate($L, $L)", writableName, getter.build());
    } else {
      CodeBlock.Builder bodyStatements = CodeBlock.builder();

      TypeElement rawBodyWriter = elementUtils.getTypeElement(BODY_WRITER);
      ParameterizedTypeName bodyWriter = ParameterizedTypeName.get(ClassName.get(rawBodyWriter), writerType);

      bodyStatements.add("\r\n$T __bodyWriter = __recWriter.completeHeader(__numSlots);\n\r", bodyWriter);

      for (FieldModel bodyField : fields.body.getFields()) {

        boolean isRuntimeLookup = bodyField.getModel().isUnresolved();
        String fieldName = bodyField.propertyName();
        TypeMirror rawType = bodyField.getElement().asType();
        String writableName = context.getFormatter().writableName(bodyField.propertyName());

        CodeBlock init = isRuntimeLookup ? CodeBlock.of("$L", context.getFormatter().writableName(bodyField.propertyName())) : CodeBlock.of("$L", writableName);

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
    Elements elementUtils = context.getElementUtils();

    TypeElement noSlots = elementUtils.getTypeElement(HEADER_NO_SLOTS);
    List<FieldModel> headerFields = fields.headerSpec.headerFields;
    FieldModel tagBody = fields.headerSpec.tagBody;

    CodeBlock.Builder headerBlock = CodeBlock.builder();
    headerBlock.add("$L", noSlots);

    for (int i = headerFields.size() - 1; i >= 0; i--) {
      FieldModel fieldModel = headerFields.get(i);
      boolean isRuntimeLookup = fieldModel.getModel().isUnresolved();
      String writableName = context.getFormatter().writableName(fieldModel.propertyName());

      CodeBlock.Builder getter = CodeBlock.builder();
      fieldModel.getAccessor().writeGet(getter, "from");
      headerBlock.add(
          "\n.prepend($S, $L, $L)",
          fieldModel.propertyName(),
          isRuntimeLookup ? context.getFormatter().writableName(fieldModel.propertyName()) : writableName,
          getter.build());
    }

    if (tagBody == null) {
      headerBlock.add("\n.simple()");
    } else {
      CodeBlock.Builder getter = CodeBlock.builder();
      String writableName = context.getFormatter().writableName(tagBody.propertyName());
      tagBody.getAccessor().writeGet(getter, "from");
      headerBlock.add("\n.withBody($L, $L)", writableName, getter.build());
    }

    return headerBlock.build();
  }

  @Override
  public CodeBlock writeIntoBody() {
    Elements elementUtils = context.getElementUtils();

    TypeElement rawHeaderWriter = elementUtils.getTypeElement(HEADER_WRITER);
    ParameterizedTypeName headerWriter = ParameterizedTypeName.get(ClassName.get(rawHeaderWriter), writerType);

    return buildInit()
        .addStatement("int __numAttrs = $L", fields.headerSpec.attributes.size())
        .addStatement("$T __recWriter = structuralWriter.record(__numAttrs)", headerWriter)
        .add(writeAttrs())
        .add(writeSlots())
        .build();
  }

}
