package ai.swim.structure.processor.writer.recognizer;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static ai.swim.structure.processor.writer.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

public class TypeVarFieldInitializer implements Emitter {
  private final FieldModel fieldModel;

  public TypeVarFieldInitializer(FieldModel fieldModel) {
    this.fieldModel = fieldModel;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);
    DeclaredType typedBuilder = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldModel.type(processingEnvironment));

    return CodeBlock.builder().addStatement("new $T(requireNonNullElse($L, ai.swim.structure.recognizer.proxy.TypeParameter.<$T>untyped()).build())", typedBuilder, nameFactory.typeParameterName(fieldModel.type(processingEnvironment).toString()), fieldModel.type(processingEnvironment)).build();
  }
}
