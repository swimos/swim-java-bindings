package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

public interface Emitter {

  CodeBlock emit(ScopedContext context);

}
