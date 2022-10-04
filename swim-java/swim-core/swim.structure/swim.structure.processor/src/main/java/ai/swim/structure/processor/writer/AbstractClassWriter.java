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

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.schema.ClassSchema;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.function.BiConsumer;

import static ai.swim.structure.processor.writer.Lookups.WRITER_EXCEPTION;
import static ai.swim.structure.processor.writer.Lookups.WRITER_PROXY;

public class AbstractClassWriter extends ClassWriter {
  public AbstractClassWriter(ScopedContext scopedContext, ClassSchema classSchema) {
    super(scopedContext, classSchema);
  }

  @Override
  public CodeBlock writeIntoBody() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement writerException = elementUtils.getTypeElement(WRITER_EXCEPTION);
    List<Model> subTypes = classSchema.getClassMap().getSubTypes();

    CodeBlock.Builder body = CodeBlock.builder();

    TriConsumer<BiConsumer<String, Object[]>, Model, String> writeFn = (controlFn, model, controlKind) -> {
      TypeMirror erasedType = typeUtils.erasure(model.type(processingEnvironment));
      controlFn.accept("$L (from instanceof $T)", new Object[] {controlKind, erasedType});
      body.addStatement("$L.getProxy().lookup($T.class).writeInto(($L) from, structuralWriter)", WRITER_PROXY, erasedType, erasedType);
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
