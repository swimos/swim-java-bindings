package ai.swim.structure.processor.structure.accessor;

import ai.swim.structure.processor.FieldView;
import com.squareup.javapoet.CodeBlock;

public class FieldAccessor extends Accessor {
  private final FieldView field;

  public FieldAccessor(FieldView field) {
    this.field = field;
  }

  @Override
  public void write(CodeBlock.Builder builder, Object arg){
    builder.add("$L = $L", this.field.getName(), arg);
  }

  @Override
  public String toString() {
    return "FieldAccessor{" +
        "field=" + field +
        '}';
  }
}
