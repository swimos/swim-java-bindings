package ai.swim.structure.processor.writer;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.TypeParameterElement;
import java.util.List;
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

  public static List<TypeVariableName> typeParametersToTypeVariable(List<? extends TypeParameterElement> typeParameters) {
    return typeParameters.stream().map(tp -> {
      TypeName[] bounds = tp.getBounds().stream().map(TypeName::get).toList().toArray(new TypeName[]{});
      return TypeVariableName.get(tp.asType().toString(), bounds);
    }).toList();
  }

}
