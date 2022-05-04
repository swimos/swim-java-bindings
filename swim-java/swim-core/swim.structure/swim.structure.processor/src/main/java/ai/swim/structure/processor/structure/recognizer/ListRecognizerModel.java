package ai.swim.structure.processor.structure.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class ListRecognizerModel extends ClassRecognizerModel {
  private final RecognizerModel delegate;

  private ListRecognizerModel(RecognizerModel delegate) {
    this.delegate = delegate;
  }

  public static ClassRecognizerModel from(VariableElement element, ScopedContext context) {
    DeclaredType variableType = (DeclaredType) element.asType();
    List<? extends TypeMirror> typeArguments = variableType.getTypeArguments();

    if (typeArguments.size() != 1) {
      throw new IllegalArgumentException("Attempted to build a list type that has more than one type parameter");
    }

    TypeMirror typeMirror = typeArguments.get(0);

    if (typeMirror.getKind() == TypeKind.DECLARED) {
      RecognizerModel delegate = context.getRecognizerFactory().lookup(typeMirror);

      if (delegate == null) {
        return new ListRecognizerModel(RuntimeRecognizerLookupModel.instance());
      } else {
        return new ListRecognizerModel(delegate);
      }
    } else {
      return new ListRecognizerModel(RuntimeRecognizerLookupModel.instance());
    }
  }

  @Override
  public String initializer() {
    return String.format("new ai.swim.structure.recognizer.std.ListRecognizer<>(%s, false)", this.delegate.initializer());
  }

}
