package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RuntimeLookup extends RecognizerModel {
  private final RecognizerModel[] parameters;

  public RuntimeLookup(TypeMirror mirror, RecognizerModel[] parameters) {
    super(mirror, ModelKind.RuntimeLookup);
    this.parameters = parameters;
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeMirror erasure = typeUtils.erasure(type);

    String typeParameters = "";

    if (parameters != null) {
      typeParameters = Arrays.stream(parameters).map(ty -> {
        if (inConstructor) {
          if (ty.kind == ModelKind.Untyped) {
            return String.format("TypeParameter.from(() -> %s)", ty.initializer(context, false));
          } else {
            return ty.initializer(context, true).toString();
          }
        } else {
          return String.format("TypeParameter.from(() -> %s)", ty.initializer(context, false));
        }
      }).collect(Collectors.joining(", "));
    }

    typeParameters = typeParameters.isBlank() ? "" : ", " + typeParameters;

    return CodeBlock.of("getProxy().lookup((Class<$T>) (Class<?>) $T.class $L)", type, erasure, typeParameters);
  }
}
