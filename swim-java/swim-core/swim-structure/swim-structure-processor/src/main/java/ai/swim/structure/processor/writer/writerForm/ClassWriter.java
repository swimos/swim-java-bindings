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

package ai.swim.structure.processor.writer.writerForm;

import ai.swim.structure.annotations.AutoloadedWriter;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import static ai.swim.structure.processor.writer.writerForm.Lookups.STRUCTURAL_WRITER_CLASS;
import static ai.swim.structure.processor.writer.writerForm.Lookups.WRITABLE_CLASS;
import static ai.swim.structure.processor.writer.writerForm.Lookups.WRITABLE_WRITE_INTO;

/**
 * Abstract class writer for shared functionality between concrete and abstract class writables.
 */
public abstract class ClassWriter {
  protected static final TypeVariableName writerType = TypeVariableName.get("__Writable_Ty__");
  protected final WriterContext context;
  private final TypeElement root;

  protected ClassWriter(TypeElement root, WriterContext context) {
    this.root = root;
    this.context = context;
  }

  /**
   * Builds this class writer specification into a {@link TypeSpec}.
   */
  public TypeSpec build() {
    Elements elementUtils = context.getElementUtils();
    Types typeUtils = context.getTypeUtils();

    AnnotationSpec writerAnnotationSpec = AnnotationSpec.builder(AutoloadedWriter.class)
        .addMember("value", "$T.class", typeUtils.erasure(root.asType()))
        .build();
    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getFormatter().writerClassName())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(writerAnnotationSpec);

    DeclaredType declaredType = (DeclaredType) root.asType();
    for (TypeMirror typeArgument : declaredType.getTypeArguments()) {
      classSpec.addTypeVariable((TypeVariableName) TypeVariableName.get(typeArgument));
    }

    TypeElement writableTypeElement = elementUtils.getTypeElement(WRITABLE_CLASS);
    DeclaredType writableType = typeUtils.getDeclaredType(writableTypeElement, root.asType());

    classSpec.addSuperinterface(TypeName.get(writableType));
    classSpec.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
    classSpec.addMethod(buildWriteInto());
    classSpec.addFields(getFields());

    return classSpec.build();
  }

  private MethodSpec buildWriteInto() {
    Elements elementUtils = context.getElementUtils();

    MethodSpec.Builder builder = MethodSpec.methodBuilder(WRITABLE_WRITE_INTO)
        .addModifiers(Modifier.PUBLIC);

    TypeElement writableTypeElement = elementUtils.getTypeElement(STRUCTURAL_WRITER_CLASS);

    builder.addTypeVariable(writerType);
    builder.returns(writerType);
    builder.addAnnotation(Override.class);
    builder.addParameter(ParameterSpec.builder(TypeName.get(root.asType()), "from").build());
    builder.addParameter(ParameterSpec
                             .builder(
                                 ParameterizedTypeName.get(ClassName.get(writableTypeElement), writerType),
                                 "structuralWriter")
                             .build());

    return builder.addCode(writeIntoBody()).build();
  }

  /**
   * Returns an iterable over the fields that this writable should contain.
   */
  public abstract Iterable<FieldSpec> getFields();

  /**
   * Returns a code block for the body of the write into method.
   */
  public abstract CodeBlock writeIntoBody();

}
