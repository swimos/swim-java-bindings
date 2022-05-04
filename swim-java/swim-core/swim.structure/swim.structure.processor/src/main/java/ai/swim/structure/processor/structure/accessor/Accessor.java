package ai.swim.structure.processor.structure.accessor;

import com.squareup.javapoet.CodeBlock;

public abstract class Accessor  {

  public abstract void write(CodeBlock.Builder builder, Object arg);

  @Override
  public abstract String toString();
}
