package ai.swim.structure.processor.writer;

import com.squareup.javapoet.CodeBlock;

/**
 * An abstract class that implementors may use for interfacing with {@link CodeBlock}'s. This serves as a shorthand
 * operator as opposed to pushing individual {@link CodeBlock}'s into one another as the {@link CodeBlock} will call
 * {@code toString} on this emitter.
 */
public abstract class Emitter {
  @Override
  public abstract String toString();
}
