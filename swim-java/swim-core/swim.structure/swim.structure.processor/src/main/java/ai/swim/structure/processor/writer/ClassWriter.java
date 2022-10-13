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

import ai.swim.structure.annotations.AutoloadedWriter;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.ClassMap;
import ai.swim.structure.processor.models.InterfaceMap;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.schema.ClassSchema;
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
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;

import static ai.swim.structure.processor.writer.Lookups.STRUCTURAL_WRITER_CLASS;
import static ai.swim.structure.processor.writer.Lookups.WRITABLE_CLASS;
import static ai.swim.structure.processor.writer.Lookups.WRITABLE_WRITE_INTO;
import static ai.swim.structure.processor.writer.Lookups.WRITER_PROXY;

public abstract class ClassWriter {
  protected static final TypeVariableName writerType = TypeVariableName.get("__Writable_Ty__");
  protected final ScopedContext context;

  protected ClassWriter(ScopedContext scopedContext) {
    this.context = scopedContext;
  }

  public static void writeClass(Model model, ScopedContext context) throws IOException {
    TypeSpec typeSpec;
    String declaredPackage;

    if (model.isClass()) {
      ClassMap classMap = (ClassMap) model;
      declaredPackage = classMap.getDeclaredPackage().getQualifiedName().toString();
      if (classMap.isAbstract()) {
        typeSpec = new AbstractClassWriter(context, classMap.getSubTypes()).build();
      } else {
        ClassSchema classSchema = ClassSchema.fromMap((ClassMap) model);
        typeSpec = new ConcreteClassWriter(context, classSchema).build();
      }
    } else if (model.isInterface()) {
      InterfaceMap interfaceMap = (InterfaceMap) model;
      declaredPackage = interfaceMap.getDeclaredPackage().getQualifiedName().toString();
      typeSpec = new AbstractClassWriter(context, interfaceMap.getSubTypes()).build();
    } else {
      throw new RuntimeException("Unimplemented schema type: " + model);
    }

    JavaFile javaFile = JavaFile.builder(declaredPackage, typeSpec)
        .addStaticImport(ClassName.bestGuess(WRITER_PROXY), "getProxy")
        .build();
    javaFile.writeTo(context.getProcessingEnvironment().getFiler());
  }

  public TypeSpec build() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    AnnotationSpec writerAnnotationSpec = AnnotationSpec.builder(AutoloadedWriter.class)
        .addMember("value", "$T.class", typeUtils.erasure(context.getRoot().asType()))
        .build();
    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getNameFactory().writerClassName())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(writerAnnotationSpec);

    DeclaredType declaredType = (DeclaredType) context.getRoot().asType();
    for (TypeMirror typeArgument : declaredType.getTypeArguments()) {
      classSpec.addTypeVariable((TypeVariableName) TypeVariableName.get(typeArgument));
    }

    TypeElement writableTypeElement = elementUtils.getTypeElement(WRITABLE_CLASS);
    DeclaredType writableType = typeUtils.getDeclaredType(writableTypeElement, context.getRoot().asType());

    classSpec.addSuperinterface(TypeName.get(writableType));
    classSpec.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
    classSpec.addMethod(buildWriteInto());
    classSpec.addFields(getFields());

    return classSpec.build();
  }

  private MethodSpec buildWriteInto() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    MethodSpec.Builder builder = MethodSpec.methodBuilder(WRITABLE_WRITE_INTO)
        .addModifiers(Modifier.PUBLIC);

    TypeElement writableTypeElement = elementUtils.getTypeElement(STRUCTURAL_WRITER_CLASS);

    builder.addTypeVariable(writerType);
    builder.returns(writerType);
    builder.addAnnotation(Override.class);
    builder.addParameter(ParameterSpec.builder(TypeName.get(context.getRoot().asType()), "from").build());
    builder.addParameter(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(writableTypeElement), writerType), "structuralWriter").build());

    return builder.addCode(writeIntoBody()).build();
  }

  public abstract Iterable<FieldSpec> getFields();

  public abstract CodeBlock writeIntoBody();

}
