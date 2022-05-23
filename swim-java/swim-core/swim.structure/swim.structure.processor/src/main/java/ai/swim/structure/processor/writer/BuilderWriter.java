package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderFields;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

import static ai.swim.structure.processor.writer.Recognizer.RECOGNIZING_BUILDER_CLASS;

public class BuilderWriter {

  public static void write(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement classRecognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    DeclaredType classRecognizingBuilderType = typeUtils.getDeclaredType(classRecognizingBuilderElement, context.getRoot().asType());

    ClassBuilder classBuilder = new ClassBuilder(schema, context);
    parentSpec.addType(classBuilder.build(TypeName.get(classRecognizingBuilderType)));

    if (!schema.getPartitionedFields().headerFields.headerFields.isEmpty()) {
      ClassName classType = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());
      TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
      ParameterizedTypeName headerBuilderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), classType);

      HeaderBuilder headerBuilder = new HeaderBuilder(schema, context);
      parentSpec.addType(headerBuilder.build(headerBuilderType));

      TypeSpec.Builder headerClass = TypeSpec.classBuilder(context.getNameFactory().headerClassName()).addModifiers(Modifier.PRIVATE, Modifier.FINAL);

      HeaderFields headerFieldSet = schema.getPartitionedFields().headerFields;
      List<FieldModel> headerFields = headerFieldSet.headerFields;

      if (headerFieldSet.tagBody != null) {
        headerFields.add(headerFieldSet.tagBody);
      }

      for (FieldModel headerField : headerFields) {
        headerClass.addField(FieldSpec.builder(TypeName.get(headerField.type()), headerField.fieldName()).build());
      }
      parentSpec.addType(headerClass.build());
    }
  }

}
