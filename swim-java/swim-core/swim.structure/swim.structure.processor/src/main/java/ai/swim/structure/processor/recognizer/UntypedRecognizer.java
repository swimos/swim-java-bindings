package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static ai.swim.structure.processor.writer.Lookups.UNTYPED_RECOGNIZER;

public class UntypedRecognizer extends StructuralRecognizer {

  public UntypedRecognizer(TypeMirror typeMirror) {
    super(typeMirror, ModelKind.Untyped);
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    if (inConstructor) {
      NameFactory nameFactory = context.getNameFactory();
      TypeVariable typeVariable = (TypeVariable) type;
      return CodeBlock.of("$L.build()", nameFactory.typeParameterName(typeVariable.toString()));
    } else {
      ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
      Elements elementUtils = processingEnvironment.getElementUtils();
      Types typeUtils = processingEnvironment.getTypeUtils();

      TypeElement typeElement = elementUtils.getTypeElement(UNTYPED_RECOGNIZER);
      DeclaredType declaredType = typeUtils.getDeclaredType(typeElement, type);

      return CodeBlock.of("new $T()", declaredType);
    }
  }
}
