package ai.swim.structure.processor.structure.accessor;

import com.squareup.javapoet.CodeBlock;

import javax.lang.model.element.ExecutableElement;

public class MethodAccessor extends Accessor {
  private final ExecutableElement method;

  public MethodAccessor(ExecutableElement method) {
    this.method = method;
  }

  @Override
  public void write(CodeBlock.Builder builder, Object arg) {
    builder.add("$L($L)", this.method.getSimpleName(), arg);
  }

  @Override
  public String toString() {
    return "MethodAccessor{" +
        "method=" + method +
        '}';
  }
}
