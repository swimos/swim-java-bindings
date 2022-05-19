package ai.swim.structure.processor.writer;

import com.squareup.javapoet.CodeBlock;

import java.util.function.BiFunction;

public class WriterUtils {

  public static void writeIndexSwitchBlock(CodeBlock.Builder body, String switchOn, int startAt, BiFunction<Integer, Integer, String> caseWriter) {
    body.beginControlFlow("switch ($L)", switchOn);

    int i = startAt;

    while (true) {
      String caseStatement = caseWriter.apply(startAt, i);
      if (caseStatement == null) {
        break;
      } else {
        body.add(caseStatement);
        i += 1;
      }
    }

    body.add("default:");
    body.addStatement("\tthrow new RuntimeException(\"Unexpected key: \" + key)");
    body.endControlFlow();
  }

}
