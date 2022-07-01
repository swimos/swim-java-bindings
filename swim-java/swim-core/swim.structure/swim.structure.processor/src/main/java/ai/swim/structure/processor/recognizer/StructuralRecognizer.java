package ai.swim.structure.processor.recognizer;

import javax.lang.model.type.TypeMirror;

public abstract class StructuralRecognizer extends RecognizerInstance {

  protected StructuralRecognizer(TypeMirror typeMirror, ModelKind kind) {
    super(typeMirror, kind, String.format("new %s()", typeMirror.toString()));
  }

  protected StructuralRecognizer(TypeMirror typeMirror) {
    this(typeMirror, ModelKind.Structural);
  }

}
