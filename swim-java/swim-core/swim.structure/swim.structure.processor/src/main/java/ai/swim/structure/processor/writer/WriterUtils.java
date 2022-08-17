package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.Lookups.TYPE_PARAMETER;


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
      TypeName[] bounds = tp.getBounds().stream().map(TypeName::get).collect(Collectors.toList()).toArray(new TypeName[]{});
      return TypeVariableName.get(tp.asType().toString(), bounds);
    }).collect(Collectors.toList());
  }

  public static List<ParameterSpec> writeGenericRecognizerConstructor(List<? extends TypeParameterElement> typeParameters, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    List<ParameterSpec> parameters = new ArrayList<>(typeParameters.size());
    TypeElement typeParameterElement = elementUtils.getTypeElement(TYPE_PARAMETER);

    for (TypeParameterElement typeParameter : typeParameters) {
      DeclaredType typed = typeUtils.getDeclaredType(typeParameterElement, typeParameter.asType());
      parameters.add(ParameterSpec.builder(TypeName.get(typed), nameFactory.typeParameterName(typeParameter.toString())).build());
    }

    return parameters;
  }

}
