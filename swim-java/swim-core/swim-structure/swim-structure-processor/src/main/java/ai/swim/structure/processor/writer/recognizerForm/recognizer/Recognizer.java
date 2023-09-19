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

package ai.swim.structure.processor.writer.recognizerForm.recognizer;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;
import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.model.InvalidModelException;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.model.TypeInitializer;
import ai.swim.structure.processor.schema.HeaderSpec;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.WriterUtils;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerContext;
import ai.swim.structure.processor.writer.recognizerForm.RecognizerNameFormatter;
import ai.swim.structure.processor.writer.recognizerForm.builder.BuilderWriter;
import ai.swim.structure.processor.writer.recognizerForm.builder.classBuilder.BindEmitter;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import static ai.swim.structure.processor.writer.WriterUtils.typeParametersToTypeVariable;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.DELEGATE_CLASS_RECOGNIZER;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.DELEGATE_ORDINAL_ATTR_KEY;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.ENUM_TAG_SPEC;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.FIELD_TAG_SPEC;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.FIXED_TAG_SPEC;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.LABELLED_ATTR_FIELD_KEY;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.LABELLED_CLASS_RECOGNIZER;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.LABELLED_ITEM_FIELD_KEY;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZER_CLASS;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZER_PROXY;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZING_BUILDER_BIND;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.STRUCTURAL_RECOGNIZER_CLASS;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.TYPE_PARAMETER;
import static ai.swim.structure.processor.writer.recognizerForm.Lookups.TYPE_READ_EVENT;
import static ai.swim.structure.processor.writer.recognizerForm.recognizer.PolymorphicRecognizer.buildPolymorphicRecognizer;

/**
 * Core recognizer writer functionality.
 */
public class Recognizer {

  /**
   * Writes a recognizer for the provided class model.
   *
   * @param model   that a recognizer will be derived for.
   * @param context recognizer scoped context to the root processing element.
   * @throws IOException if there is a failure to write the recognizer to disk.
   */
  public static void writeRecognizer(ClassLikeModel model, RecognizerContext context) throws IOException {
    List<Model> subTypes = model.getSubTypes();
    TypeSpec typeSpec;

    PartitionedFields fields = PartitionedFields.buildFrom(model.getFields());

    if (model.isAbstract()) {
      // Write an abstract class recognizer
      typeSpec = buildPolymorphicRecognizer(context, subTypes).build();
    } else if (model.isClass()) {
      boolean isPolymorphic = !subTypes.isEmpty();
      TypeSpec.Builder concreteRecognizer = writeClassRecognizer(
          isPolymorphic,
          model,
          fields,
          context,
          new ClassTransposition(model, fields));
      if (isPolymorphic) {
        // The class is not abstract but does have subtypes. So we need to write an abstract class recognizer that also
        // has an extra 'subtype' that is the concrete class (the root processing element).

        subTypes.add(new Model(null, null, null) {
          @Override
          public InitializedType instantiate(TypeInitializer initializer,
              boolean inConstructor) throws InvalidModelException {
            return new InitializedType(
                null,
                CodeBlock.of("new $L()", context.getFormatter().concreteRecognizerClassName()));
          }

          @Override
          public String toString() {
            return null;
          }
        });

        TypeSpec.Builder classRecognizer = buildPolymorphicRecognizer(context, subTypes);
        classRecognizer.addType(concreteRecognizer.build());
        typeSpec = classRecognizer.build();
      } else {
        typeSpec = concreteRecognizer.build();
      }
    } else if (model.isEnum()) {
      // Write a class recognizer for the enum.
      typeSpec = writeClassRecognizer(false, model, fields, context, new EnumTransposition(model, fields)).build();
    } else {
      throw new AssertionError("Unhandled class map type: " + model.getClass().getCanonicalName());
    }

    JavaFile javaFile = JavaFile
        .builder(model.getDeclaredPackage().getQualifiedName().toString(), typeSpec)
        .addStaticImport(Objects.class, "requireNonNullElse")
        .addStaticImport(ClassName.bestGuess(RECOGNIZER_PROXY), "getProxy")
        .build();
    javaFile.writeTo(context.getProcessingEnvironment().getFiler());
  }

  private static TypeSpec.Builder writeClassRecognizer(boolean isPolymorphic,
      ClassLikeModel model,
      PartitionedFields fields,
      RecognizerContext context,
      Transposition transposition) {
    RecognizerNameFormatter formatter = context.getFormatter();
    Modifier modifier = isPolymorphic ? Modifier.PRIVATE : Modifier.PUBLIC;
    String className = isPolymorphic ? formatter.concreteRecognizerClassName() : formatter.recognizerClassName();

    TypeSpec.Builder classSpec = TypeSpec
        .classBuilder(className)
        .addModifiers(modifier, Modifier.FINAL)
        .addTypeVariables(typeParametersToTypeVariable(model.getTypeParameters()));

    if (isPolymorphic) {
      classSpec.addModifiers(Modifier.STATIC);
    } else {
      AnnotationSpec recognizerAnnotationSpec = AnnotationSpec
          .builder(AutoloadedRecognizer.class)
          .addMember("value", "$L.class", model.qualifiedName())
          .build();
      classSpec.addAnnotation(recognizerAnnotationSpec);
    }

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement superclassRecognizerTypeElement = elementUtils.getTypeElement(STRUCTURAL_RECOGNIZER_CLASS);
    DeclaredType superclassRecognizerType = typeUtils.getDeclaredType(
        superclassRecognizerTypeElement,
        context.getRoot().asType());
    TypeName superclassRecognizerTypeName = TypeName.get(superclassRecognizerType);

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    ParameterizedTypeName recognizerTypeName = ParameterizedTypeName.get(
        ClassName.get(recognizerTypeElement),
        transposition.builderType(context));

    classSpec.superclass(superclassRecognizerTypeName);
    classSpec.addField(FieldSpec.builder(recognizerTypeName, "recognizer", Modifier.PRIVATE).build());
    classSpec.addField(transposition.tagSpec(context));
    classSpec.addMethods(buildConstructors(model, fields, context, transposition));
    classSpec.addMethods(buildMethods(context, className, transposition));

    BuilderWriter.write(classSpec, model, fields, context, transposition);

    TypeSpec nested = transposition.nested(context);
    if (nested != null) {
      classSpec.addType(nested);
    }

    return classSpec;
  }

  private static List<MethodSpec> buildMethods(RecognizerContext context,
      String className,
      Transposition transposition) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    TypeElement typeElement = elementUtils.getTypeElement(TYPE_READ_EVENT);

    List<MethodSpec> methods = new ArrayList<>();

    methods.add(buildPolymorphicMethod(
        TypeName.get(typedRecognizer),
        "feedEvent",
        ParameterSpec.builder(TypeName.get(typeElement.asType()), "event").build(),
        CodeBlock.of("this.recognizer = this.recognizer.feedEvent(event);" + "\n\tif (this.recognizer.isError()) {\nreturn Recognizer.error(this.recognizer.trap());" + "\n}" + "\nreturn this;")));
    methods.add(buildPolymorphicMethod(
        TypeName.get(boolean.class),
        "isCont",
        null,
        CodeBlock.of("return this.recognizer.isCont();")));
    methods.add(buildPolymorphicMethod(
        TypeName.get(boolean.class),
        "isDone",
        null,
        CodeBlock.of("return this.recognizer.isDone();")));
    methods.add(buildPolymorphicMethod(
        TypeName.get(boolean.class),
        "isError",
        null,
        CodeBlock.of("return this.recognizer.isError();")));
    methods.add(buildPolymorphicMethod(
        TypeName.get(RuntimeException.class),
        "trap",
        null,
        CodeBlock.of("return this.recognizer.trap();")));
    methods.add(buildPolymorphicMethod(
        TypeName.get(typedRecognizer),
        "reset",
        null,
        CodeBlock.of("return new $L();", className)));
    methods.add(buildPolymorphicMethod(
        TypeName.get(typedRecognizer),
        "asBodyRecognizer",
        null,
        CodeBlock.of("return this;")));
    methods.add(transposition.classBind(context));

    return methods;
  }

  static MethodSpec buildPolymorphicMethod(TypeName returns, String name, ParameterSpec parameterSpec, CodeBlock body) {
    MethodSpec.Builder builder = MethodSpec
        .methodBuilder(name)
        .returns(returns)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addCode(body);

    if (parameterSpec != null) {
      builder.addParameter(parameterSpec);
    }

    return builder.build();
  }

  private static List<MethodSpec> buildConstructors(ClassLikeModel model,
      PartitionedFields fields,
      RecognizerContext context,
      Transposition transposition) {
    List<MethodSpec> constructors = new ArrayList<>();
    constructors.add(buildDefaultConstructor(model, context));
    constructors.add(buildBuilderConstructor(fields, context, transposition));

    if (!model.getTypeParameters().isEmpty()) {
      constructors.add(buildTypedConstructor(model, context));
    }

    return constructors;
  }

  /**
   * Builds the default, zero-arg, constructor for this recognizer.
   */
  private static MethodSpec buildDefaultConstructor(ClassLikeModel model, RecognizerContext context) {
    String recognizers = model
        .getTypeParameters()
        .stream()
        .map(ty -> "ai.swim.structure.recognizer.proxy.RecognizerTypeParameter.untyped()")
        .collect(Collectors.joining(", "));
    return MethodSpec
        .constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addCode("this(new $L($L));", context.getFormatter().builderClassName(), recognizers)
        .build();
  }

  /**
   * Builds a typed constructor:
   * <pre>
   *   {@code
   *   @AutoForm.TypedConstructor
   *   public GenericBodyRecognizer(RecognizerTypeParameter<N> nType) {
   *     this(new GenericBodyBuilder<>(requireNonNullElse(nType, RecognizerTypeParameter.<N>untyped())));
   *   }
   *   }
   * </pre>
   */
  private static MethodSpec buildTypedConstructor(ClassLikeModel model, RecognizerContext context) {
    MethodSpec.Builder methodBuilder = MethodSpec
        .constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AutoForm.TypedConstructor.class);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    RecognizerNameFormatter formatter = context.getFormatter();

    List<? extends TypeParameterElement> typeParameters = model.getTypeParameters();
    List<ParameterSpec> parameters = new ArrayList<>(typeParameters.size());
    TypeElement typeParameterElement = elementUtils.getTypeElement(TYPE_PARAMETER);

    StringJoiner nullChecks = new StringJoiner(",\n");

    for (TypeParameterElement typeParameter : typeParameters) {
      DeclaredType typed = typeUtils.getDeclaredType(typeParameterElement, typeParameter.asType());
      String typeParameterName = formatter.typeParameterName(typeParameter.toString());
      parameters.add(ParameterSpec.builder(TypeName.get(typed), typeParameterName).build());

      nullChecks.add(CodeBlock
                         .of(
                             "requireNonNullElse($L, ai.swim.structure.recognizer.proxy.RecognizerTypeParameter.<$T>untyped())",
                             typeParameterName,
                             typeParameter)
                         .toString());
    }

    CodeBlock body = CodeBlock.of(
        "this(new $L<>($L));",
        context.getFormatter().builderClassName(),
        nullChecks.toString());
    methodBuilder.addCode(body);

    return methodBuilder.addParameters(parameters).build();
  }

  /**
   * Builds the constructor which accepts the delegate field builder.
   * <pre>
   *   {@code
   *     private GenericBodyRecognizer(GenericBodyBuilder builder) {
   *     this.recognizer = new DelegateClassRecognizer<GenericBody<N>>(tagSpec, builder, 3, (key) -> {
   *       if (key.isHeader()) {
   *         return 0;
   *       }
   *       if (key.isFirstItem()) {
   *         return 1;
   *       }
   *       return null;
   *     }
   *     );
   *   }
   *   }
   * </pre>
   */
  private static MethodSpec buildBuilderConstructor(PartitionedFields fields,
      RecognizerContext context,
      Transposition transposition) {
    MethodSpec.Builder methodBuilder = MethodSpec
        .constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameter(ParameterSpec
                          .builder(ClassName.bestGuess(context.getFormatter().builderClassName()), "builder")
                          .build());

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    String recognizerName = fields.body.isReplaced() ? DELEGATE_CLASS_RECOGNIZER : LABELLED_CLASS_RECOGNIZER;
    CodeBlock indexFn = fields.body.isReplaced() ? buildOrdinalIndexFn(fields, context) : buildStandardIndexFn(
        fields,
        context);

    TypeElement classRecognizerElement = elementUtils.getTypeElement(recognizerName);
    ParameterizedTypeName classRecognizerDeclaredType = ParameterizedTypeName.get(
        ClassName.get(classRecognizerElement),
        transposition.builderType(context));

    CodeBlock body = CodeBlock.of(
        "this.recognizer = new $T(tagSpec, builder, $L, $L);",
        classRecognizerDeclaredType,
        fields.count(),
        indexFn);

    return methodBuilder.addCode(body).build();
  }

  private static CodeBlock buildOrdinalIndexFn(PartitionedFields fields, RecognizerContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    HeaderSpec headerFieldSet = fields.headerSpec;

    int idx = 0;
    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("(key) ->");

    if (headerFieldSet.hasTagName()) {
      idx += 1;
      body.beginControlFlow("if (key.isTag())");
      body.addStatement("return 0");
      body.endControlFlow();
    }

    if (headerFieldSet.hasTagBody() || !headerFieldSet.headerFields.isEmpty()) {
      body.beginControlFlow("if (key.isHeader())");
      body.addStatement("return $L", idx);
      body.endControlFlow();

      idx += 1;
    }

    if (!headerFieldSet.attributes.isEmpty()) {
      TypeElement attrKey = elementUtils.getTypeElement(DELEGATE_ORDINAL_ATTR_KEY);

      body.beginControlFlow("if (key.isAttr())");
      body.addStatement("$T attrKey = ($T) key", attrKey, attrKey);

      WriterUtils.writeIndexSwitchBlock(body, "attrKey.getName()", idx, (offset, i) -> {
        if (i - offset == headerFieldSet.attributes.size()) {
          return null;
        } else {
          FieldModel recognizer = headerFieldSet.attributes.get(i - offset);
          return String.format("case \"%s\":\r\n\t return %s;\r\n", recognizer.propertyName(), i);
        }
      });

      body.endControlFlow();
    }

    body.beginControlFlow("if (key.isFirstItem())");
    body.addStatement("return $L", idx + headerFieldSet.attributes.size());
    body.endControlFlow();

    body.addStatement("return null");
    body.endControlFlow();

    return body.build();
  }

  private static CodeBlock buildStandardIndexFn(PartitionedFields fields, RecognizerContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("(key) ->");

    HeaderSpec headerSpec = fields.headerSpec;

    int idx = 0;

    if (headerSpec.hasTagBody() || fields.hasHeaderFields()) {
      body.beginControlFlow("if (key.isHeader())");
      body.addStatement("return $L", idx);
      body.endControlFlow();
      idx += 1;
    }

    if (!headerSpec.attributes.isEmpty()) {
      body.beginControlFlow("if (key.isAttribute())");
      TypeElement attrFieldKeyElement = elementUtils.getTypeElement(LABELLED_ATTR_FIELD_KEY);

      body.addStatement("$T attrFieldKey = ($T) key", attrFieldKeyElement, attrFieldKeyElement);
      body.beginControlFlow("switch (attrFieldKey.getKey())");

      int attrCount = headerSpec.attributes.size();

      for (int i = 0; i < attrCount; i++) {
        FieldModel recognizer = headerSpec.attributes.get(i);

        body.add("case \"$L\":", recognizer.propertyName());
        body.addStatement("\t return $L", i + idx);
      }

      body.endControlFlow();
      body.endControlFlow();

      idx += attrCount;
    }

    List<FieldModel> items = fields.body.getFields();

    if (!items.isEmpty()) {
      body.beginControlFlow("if (key.isItem())");

      TypeElement itemFieldKeyElement = elementUtils.getTypeElement(LABELLED_ITEM_FIELD_KEY);
      body.addStatement("$T itemFieldKey = ($T) key", itemFieldKeyElement, itemFieldKeyElement);
      body.beginControlFlow("switch (itemFieldKey.getName())");

      for (int i = 0; i < items.size(); i++) {
        FieldModel recognizer = items.get(i);

        body.add("case \"$L\":", recognizer.propertyName());
        body.addStatement("\t return $L", i + idx);
      }

      body.add("default:");
      body.addStatement("\tthrow new RuntimeException(\"Unexpected key: \" + key)");
      body.endControlFlow();
      body.endControlFlow();
    }

    body.addStatement("return null");
    body.endControlFlow();

    return body.build();
  }

  /**
   * A transposition that is applied to a class, specifying how its tag, builder type, builder bind is defined as well
   * as an optional nested class that may be inserted.
   */
  public interface Transposition {
    /**
     * Builds the tag specification for the class.
     */
    FieldSpec tagSpec(RecognizerContext context);

    /**
     * Builds the bind method for the recognizer.
     */
    MethodSpec classBind(RecognizerContext context);

    /**
     * Specifies the name of the builder for the class.
     */
    TypeName builderType(RecognizerContext context);

    /**
     * Builds the bind method for the class builder.
     */
    MethodSpec builderBind(RecognizerContext context);

    /**
     * Specifies an optional, nested, class to be inserted into the parent's type specification.
     */
    TypeSpec nested(RecognizerContext context);
  }

  private static class ClassTransposition implements Transposition {
    private final ClassLikeModel model;
    private final PartitionedFields fields;

    ClassTransposition(ClassLikeModel model, PartitionedFields fields) {
      this.model = model;
      this.fields = fields;
    }

    @Override
    public FieldSpec tagSpec(RecognizerContext context) {
      String tag = model.getTag();
      Elements elementUtils = context.getProcessingEnvironment().getElementUtils();
      TypeElement fieldTagSpecElement = elementUtils.getTypeElement(tag == null ? FIELD_TAG_SPEC : FIXED_TAG_SPEC);
      CodeBlock fieldSpec = tag == null ? CodeBlock.of("new $T()", fieldTagSpecElement) : CodeBlock.of(
          "new $T(\"$L\")",
          fieldTagSpecElement,
          tag);

      return FieldSpec
          .builder(TypeName.get(fieldTagSpecElement.asType()), "tagSpec")
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .initializer(fieldSpec)
          .build();
    }

    @Override
    public MethodSpec classBind(RecognizerContext context) {
      return buildPolymorphicMethod(
          TypeName.get(context.getRoot().asType()),
          "bind",
          null,
          CodeBlock.of("return this.recognizer.bind();"));
    }

    @Override
    public TypeName builderType(RecognizerContext context) {
      return ClassName.get(context.getRoot().asType());
    }

    @Override
    public MethodSpec builderBind(RecognizerContext context) {
      return MethodSpec.methodBuilder(RECOGNIZING_BUILDER_BIND)
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override.class)
          .returns(TypeName.get(context.getRoot().asType()))
          .addCode(new BindEmitter(model, fields, context).toString())
          .build();
    }

    @Override
    public TypeSpec nested(RecognizerContext context) {
      return null;
    }
  }

  private static class EnumTransposition implements Transposition {
    private final ClassLikeModel model;
    private final PartitionedFields fields;

    EnumTransposition(ClassLikeModel model, PartitionedFields fields) {
      this.model = model;
      this.fields = fields;
    }

    @Override
    public FieldSpec tagSpec(RecognizerContext context) {
      Elements elementUtils = context.getProcessingEnvironment().getElementUtils();
      TypeElement fieldTagSpecElement = elementUtils.getTypeElement(ENUM_TAG_SPEC);
      String variantTags = model
          .getElement()
          .getEnclosedElements()
          .stream()
          .filter(e -> e.getKind().equals(ElementKind.ENUM_CONSTANT))
          .map(e -> {
            AutoForm.Tag tag = e.getAnnotation(AutoForm.Tag.class);
            if (tag != null && !tag.value().isBlank()) {
              return tag.value();
            } else {
              return e.toString();
            }
          })
          .map(v -> String.format("\"%s\"", v)).collect(Collectors.joining(", "));

      return FieldSpec
          .builder(TypeName.get(fieldTagSpecElement.asType()), "tagSpec")
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .initializer(CodeBlock.of("new $T(java.util.List.of($L))", fieldTagSpecElement, variantTags))
          .build();
    }

    @Override
    public MethodSpec classBind(RecognizerContext context) {
      RecognizerNameFormatter formatter = context.getFormatter();
      TypeName typeName = TypeName.get(context.getRoot().asType());

      CodeBlock.Builder body = CodeBlock.builder();
      body.addStatement("$T spec = recognizer.bind()", ClassName.bestGuess(formatter.enumSpec()));
      body.addStatement("$T obj = $T.valueOf(tagSpec.getEnumVariant())", typeName, typeName);

      for (FieldModel fieldModel : model.getFields()) {
        if (fieldModel.isIgnored()) {
          continue;
        }

        CodeBlock.Builder getter = CodeBlock.builder();
        fieldModel.getAccessor().writeGet(getter, "obj");

        body
            .beginControlFlow("if ($L != spec.$L)", getter.build(), fieldModel.getName())
            .addStatement(
                "throw new ai.swim.structure.recognizer.RecognizerException(String.format(\"Field mismatch. Expected '%s' but got '%s'\", spec.$L, $L))",
                fieldModel.getName(),
                getter.build())
            .endControlFlow();
      }

      return buildPolymorphicMethod(typeName, "bind", null, body.addStatement("return obj").build());
    }

    @Override
    public TypeName builderType(RecognizerContext context) {
      return ClassName.bestGuess(context.getFormatter().enumSpec());
    }

    @Override
    public MethodSpec builderBind(RecognizerContext context) {
      RecognizerNameFormatter formatter = context.getFormatter();

      CodeBlock.Builder body = CodeBlock.builder();

      if (fields.hasHeaderFields()) {
        body.addStatement(
            "$T __header = $L.bind()",
            ClassName.bestGuess(formatter.headerClassName()),
            formatter.headerBuilderFieldName());
      }

      ClassName enumTy = ClassName.bestGuess(formatter.enumSpec());

      String bindOp = model
          .getFields()
          .stream()
          .filter(f -> !f.isIgnored())
          .map(f -> {
            if (fields.isHeader(f)) {
              return String.format("__header.%s", f.getName());
            } else {
              return String.format("%s.bind()", formatter.fieldBuilderName(f.getName().toString()));
            }
          })
          .collect(Collectors.joining(", "));

      body.addStatement("return new $T($L)", enumTy, bindOp);

      return buildPolymorphicMethod(
          ClassName.bestGuess(formatter.enumSpec()),
          "bind",
          null,
          body.build()
                                   );
    }

    @Override
    public TypeSpec nested(RecognizerContext context) {
      TypeSpec.Builder builder = TypeSpec
          .classBuilder(context.getFormatter().enumSpec())
          .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
      MethodSpec.Builder constructor = MethodSpec.constructorBuilder();

      for (FieldModel fieldModel : model.getFields()) {
        if (fieldModel.isIgnored()) {
          continue;
        }

        TypeName ty = TypeName.get(fieldModel.type());
        String name = fieldModel.getName().toString();
        builder.addField(FieldSpec.builder(ty, name, Modifier.PRIVATE).build());

        constructor.addParameter(ParameterSpec.builder(ty, name).build());
        constructor.addStatement(CodeBlock.of("this.$L = $L", name, name));
      }

      return builder.addMethod(constructor.build()).build();
    }
  }

}
