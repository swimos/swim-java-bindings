package ai.swim.structure.processor.writer.builder.classBuilder;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeVariable;

public class FieldInitializer implements Emitter {
  private final FieldModel field;
  private final boolean retyped;

  public FieldInitializer(FieldModel field, boolean retyped) {
    this.field = field;
    this.retyped = retyped;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder parameter = CodeBlock.builder();

    if (!field.getFieldView().isParameterised()) {
      return parameter.add(field.initializer(context,false)).build();
    }

    NameFactory nameFactory = context.getNameFactory();
    DeclaredType fieldType = (DeclaredType) field.getFieldView().getElement().asType();

    switch (fieldType.getKind()) {
      case TYPEVAR:
        TypeVariable typeVariable = (TypeVariable) fieldType;
        return parameter.add(nameFactory.typeParameterName(typeVariable.toString())).build();
      case DECLARED:
        RecognizerModel parameterised = field.fromTypeParameters(context);
        parameter.add("TypeParameter.from(() -> $L)", parameterised.initializer(context,false)).build();
      default:
        throw new RuntimeException("Unhandled type parameter builder field type: " + fieldType.getKind());
    }
  }
}
