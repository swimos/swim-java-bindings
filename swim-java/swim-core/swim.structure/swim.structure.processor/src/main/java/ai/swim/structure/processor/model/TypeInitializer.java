package ai.swim.structure.processor.model;

import ai.swim.structure.processor.model.mapping.CoreTypeKind;

import javax.lang.model.type.TypeMirror;

public interface TypeInitializer {
  InitializedType core(CoreTypeKind typeKind);

  InitializedType arrayType(ArrayLibraryModel model, boolean inConstructor);

  InitializedType untyped(TypeMirror type, boolean inConstructor);

  InitializedType declared(Model model, boolean inConstructor, Model... parameters);

  InitializedType unresolved(Model model, boolean inConstructor) throws InvalidModelException;
}
