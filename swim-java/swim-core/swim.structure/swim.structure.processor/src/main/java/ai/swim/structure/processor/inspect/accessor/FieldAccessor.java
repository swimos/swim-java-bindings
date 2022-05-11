package ai.swim.structure.processor.inspect.accessor;

import com.squareup.javapoet.CodeBlock;

public class FieldAccessor extends Accessor {
  private final String fieldName;

  public FieldAccessor(String fieldName) {
    this.fieldName = fieldName;
  }

  @Override
  public void write(CodeBlock.Builder builder, String instance, Object arg) {
    builder.add("$L.$L = $L;\n", instance, fieldName, arg);
  }

  @Override
  public String toString() {
    return "FieldAccessor{" +
        "fieldName=" + fieldName +
        '}';
  }
}
