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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.Lookups.WRITER_EXCEPTION;

public class AbstractClassWriter extends ClassWriter {
  private final HashMap<String, String> subtypeWritableLut;
  private final List<Model> subTypes;

  public AbstractClassWriter(ScopedContext scopedContext, List<Model> subTypes) {
    super(scopedContext);
    this.subTypes = subTypes;
    this.subtypeWritableLut = mapSubTypes(scopedContext, subTypes);
  }

  private static HashMap<String, String> mapSubTypes(ScopedContext scopedContext, List<Model> subTypes) {
    NameFactory nameFactory = scopedContext.getNameFactory();
    ProcessingEnvironment processingEnvironment = scopedContext.getProcessingEnvironment();

    HashMap<String, String> lut = new HashMap<>(subTypes.size());

    for (Model ty : subTypes) {
      TypeMirror type = ty.type(processingEnvironment);
      String tyString = type.toString();
      int start = tyString.lastIndexOf('.') + 1;
      int end = tyString.indexOf('<');
      end = end == -1 ? tyString.length() : end;

      String tyName = tyString.substring(start, end);

      lut.put(type.toString(), nameFactory.writableName(tyName));
    }

    return lut;
  }

  @Override
  public Iterable<FieldSpec> getFields() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement typeElement = elementUtils.getTypeElement(Lookups.WRITABLE_CLASS);

    return subTypes.stream().map(ty -> {
      TypeMirror type = ty.type(processingEnvironment);
      TypeMirror erasedType = typeUtils.erasure(type);
      DeclaredType writableType = typeUtils.getDeclaredType(typeElement, erasedType);

      return FieldSpec.builder(TypeName.get(writableType), subtypeWritableLut.get(type.toString()), Modifier.PRIVATE).build();
    }).collect(Collectors.toList());
  }

  @Override
  public CodeBlock writeIntoBody() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement writerException = elementUtils.getTypeElement(WRITER_EXCEPTION);
    CodeBlock.Builder body = CodeBlock.builder();

    TriConsumer<BiConsumer<String, Object[]>, Model, String> writeFn = (controlFn, model, controlKind) -> {
      TypeMirror writableType = model.type(processingEnvironment);
      String writableField = subtypeWritableLut.get(writableType.toString());

      TypeMirror erasedType = typeUtils.erasure(writableType);
      controlFn.accept("$L (from instanceof $T)", new Object[] {controlKind, erasedType});

      body.beginControlFlow("if ($L == null)", writableField);
      body.addStatement("$L = getProxy().lookup($T.class)", writableField, erasedType);
      body.endControlFlow();
      body.addStatement("$L.writeInto(($L) from, structuralWriter)", writableField, erasedType);
    };

    // the direct indexing is safe as the processor checks that there is at least one subtype in the annotation.
    writeFn.accept(body::beginControlFlow, subTypes.get(0), "if");

    for (int i = 1; i < subTypes.size(); i++) {
      writeFn.accept(body::nextControlFlow, subTypes.get(i), "else if");
    }

    body.endControlFlow()
        .addStatement("throw new $T(\"Unsupported type: \" + from)", writerException);

    return body.build();
  }

  @FunctionalInterface
  interface TriConsumer<A1, A2, A3> {
    void accept(A1 a1, A2 a2, A3 a3);
  }

}
