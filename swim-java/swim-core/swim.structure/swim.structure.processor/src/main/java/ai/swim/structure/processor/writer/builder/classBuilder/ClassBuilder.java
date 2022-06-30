package ai.swim.structure.processor.writer.builder.classBuilder;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ProcessingContext;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.builder.Builder;
import ai.swim.structure.processor.writer.builder.header.HeaderIndexFn;
import ai.swim.structure.processor.writer.recognizer.TypeVarFieldInitializer;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.swim.structure.processor.writer.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.writer.Lookups.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.writer.WriterUtils.typeParametersToTypeVariable;
import static ai.swim.structure.processor.writer.WriterUtils.writeGenericRecognizerConstructor;

public class ClassBuilder extends Builder {

  private static final String TYPE_PARAMETER = "ai.swim.structure.recognizer.proxy.TypeParameter";

  public ClassBuilder(ClassSchema classSchema, ScopedContext context) {
    super(classSchema, context);
  }

  @Override
  protected TypeSpec.Builder init() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(context.getNameFactory().builderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addMethod(buildDefaultConstructor())
        .addTypeVariables(typeParametersToTypeVariable(schema.getTypeParameters()));

    if (!schema.getTypeParameters().isEmpty()) {
      builder.addMethod(buildParameterisedConstructor());
    }

    return builder;
  }

  private MethodSpec buildDefaultConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
  }

  private MethodSpec buildParameterisedConstructor() {
    MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    List<ParameterSpec> parameters = writeGenericRecognizerConstructor(schema.getTypeParameters(), context);
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel fieldModel : schema.getClassMap().getFieldModels()) {
      if (fieldModel.getFieldView().isParameterised()) {
        VariableElement element = fieldModel.getFieldView().getElement();
        TypeKind typeKind = element.asType().getKind();

        switch (typeKind) {
          case TYPEVAR:
            body.add(initialiseTypeVarField(context, fieldModel));
            break;
          case DECLARED:
            DeclaredType declaredType = (DeclaredType) element.asType();
            body.add(initialiseParameterisedField(context, fieldModel, declaredType));
            break;
          default:
            throw new AssertionError("Unexpected type kind when processing generic parameters: " + typeKind + " in " + context.getRoot());
        }
      }
    }

    return builder.addParameters(parameters)
        .addCode(body.build())
        .build();
  }

  private CodeBlock initialiseTypeVarField(ScopedContext context, FieldModel fieldModel) {
    NameFactory nameFactory = context.getNameFactory();
    String fieldBuilderName = nameFactory.fieldBuilderName(fieldModel.fieldName());

    return CodeBlock.builder().addStatement("this.$L = $L", fieldBuilderName, new TypeVarFieldInitializer(fieldModel).emit(context).toString()).build();
  }

  private CodeBlock initialiseParameterisedField(ScopedContext context, FieldModel fieldModel, DeclaredType declaredType) {
    ProcessingContext processingContext = context.getProcessingContext();
    ProcessingEnvironment processingEnvironment = processingContext.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeMirror erasedMirror = typeUtils.erasure(fieldModel.type(processingEnvironment));
    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);
    DeclaredType typedBuilder = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldModel.type(processingEnvironment));

    NameFactory nameFactory = context.getNameFactory();
    String builderName = nameFactory.fieldBuilderName(fieldModel.fieldName());

    RecognizerModel retyped = fieldModel.retyped(context);

    return CodeBlock.of("this.$L = new $T(ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookupStructural((Class<$T>) (Class<?>) $T.class, $L));\n", builderName, typedBuilder, fieldModel.type(processingEnvironment), erasedMirror, retyped.recognizerInitializer());
  }

  @Override
  protected Emitter buildBindBlock() {
    return new BindEmitter(schema);
  }

  @Override
  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = super.buildFields();

    if (schema.getPartitionedFields().hasHeaderFields()) {
      fieldSpecs.add(0, buildHeaderField());
    }

    return fieldSpecs;
  }

  private FieldSpec buildHeaderField() {
    NameFactory nameFactory = context.getNameFactory();
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    ParameterizedTypeName builderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), ClassName.bestGuess(nameFactory.headerCanonicalName()));

    FieldSpec.Builder fieldBuilder = FieldSpec.builder(
        builderType,
        nameFactory.headerBuilderFieldName(),
        Modifier.PRIVATE
    );

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    int numSlots = partitionedFields.headerSet.headerFields.size();

    boolean hasBody = partitionedFields.headerSet.hasTagBody();
    String headerBuilder = nameFactory.headerBuilderCanonicalName();

    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add(
        "$L($L, () -> new $L(), $L, $L)",
        nameFactory.headerBuilderMethod(),
        hasBody,
        headerBuilder,
        numSlots,
        new HeaderIndexFn(schema).emit(context)
    );

    return fieldBuilder.initializer(initializer.build()).build();
  }

  @Override
  protected Emitter buildFeedIndexedBlock() {
    return new FeedIndexedEmitter(schema);
  }

  @Override
  protected Emitter buildResetBlock() {
    return new ResetEmitter(schema);
  }

  @Override
  protected List<FieldModel> getFields() {
    return schema.discriminate()
        .stream()
        .filter(f -> !f.isHeader())
        .flatMap(f -> {
          FieldDiscriminate.SingleField singleField = (FieldDiscriminate.SingleField) f;
          return Stream.of(singleField.getField());
        })
        .collect(Collectors.toList());
  }
}
