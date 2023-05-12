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

import ai.swim.structure.processor.model.Model;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

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

import static ai.swim.structure.processor.writer.writerForm.Lookups.WRITER_EXCEPTION;

/**
 * Class for emitting writable implementations for abstract classes.
 * <p>
 * The emitted class will contain an uninitialized field for each subclass that it *may* serialize at some point. Once
 * a matched subclass is encountered, it will resolve the writable for that subclass and store it for use in the future.
 */
public class AbstractClassWriter extends ClassWriter {
  private final HashMap<String, String> subtypeWritableLut;
  private final List<Model> subTypes;

  public AbstractClassWriter(TypeElement root, WriterContext context, List<Model> subTypes) {
    super(root, context);
    this.subTypes = subTypes;
    this.subtypeWritableLut = mapSubTypes(context.getFormatter(), subTypes);
  }

  /**
   * Maps class subtype names to a name without type parameters. Such as {@code ValueClass<T> -> ValueClass}.
   */
  private static HashMap<String, String> mapSubTypes(WriterNameFormatter formatter, List<Model> subTypes) {
    HashMap<String, String> lut = new HashMap<>(subTypes.size());

    for (Model ty : subTypes) {
      TypeMirror type = ty.getType();
      String tyString = type.toString();
      int start = tyString.lastIndexOf('.') + 1;
      int end = tyString.indexOf('<');
      end = end == -1 ? tyString.length() : end;

      String tyName = tyString.substring(start, end);
      lut.put(type.toString(), formatter.writableName(tyName));
    }

    return lut;
  }

  @Override
  public Iterable<FieldSpec> getFields() {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();

    TypeElement typeElement = elementUtils.getTypeElement(Lookups.WRITABLE_CLASS);

    return subTypes.stream().map(ty -> {
      TypeMirror type = ty.getType();
      TypeMirror erasedType = typeUtils.erasure(type);
      DeclaredType writableType = typeUtils.getDeclaredType(typeElement, erasedType);

      return FieldSpec.builder(TypeName.get(writableType), subtypeWritableLut.get(type.toString()), Modifier.PRIVATE).build();
    }).collect(Collectors.toList());
  }

  @Override
  public CodeBlock writeIntoBody() {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();

    TypeElement writerException = elementUtils.getTypeElement(WRITER_EXCEPTION);
    CodeBlock.Builder body = CodeBlock.builder();

    TriConsumer<BiConsumer<String, Object[]>, Model, String> writeFn = (controlFn, model, controlKind) -> {
      TypeMirror writableType = model.getType();
      String writableField = subtypeWritableLut.get(writableType.toString());

      TypeMirror erasedType = typeUtils.erasure(writableType);
      controlFn.accept("$L (from instanceof $T)", new Object[]{controlKind, erasedType});

      body.beginControlFlow("if ($L == null)", writableField);
      body.addStatement("$L = getProxy().lookup($T.class)", writableField, erasedType);
      body.endControlFlow();
      body.addStatement("return $L.writeInto(($L) from, structuralWriter)", writableField, erasedType);
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
