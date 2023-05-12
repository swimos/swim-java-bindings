package ai.swim.structure.processor.model;

import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.TypeMirror;

public class InitializedType {
  private final TypeMirror mirror;
  private final CodeBlock initializer;

  public InitializedType(TypeMirror mirror, CodeBlock initializer) {
    this.mirror = mirror;
    this.initializer = initializer;
  }

  @Override
  public String toString() {
    return initializer.toString();
  }

  public CodeBlock getInitializer() {
    return initializer;
  }

  public TypeMirror getMirror() {
    return mirror;
  }
}
