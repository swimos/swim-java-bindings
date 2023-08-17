// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.writer;

import ai.swim.structure.Recon;
import ai.swim.structure.value.Value;
import ai.swim.structure.writer.print.strategy.PrettyPrintStrategy;
import ai.swim.structure.writer.print.strategy.PrintStrategy;
import ai.swim.structure.writer.value.ValueStructuralWriter;
import java.io.StringWriter;

/**
 * An interface for transforming objects into another representation using a {@link StructuralWriter}; such as
 * converting an object into a Recon string or a {@link Value}. This interface defines an abstract transformation from
 * {@code F} to {@code T}.
 * <p>
 * {@link Writable} implementations should be stateless, threadsafe, reusable and automatically resolve any type
 * parameters incrementally.
 * <p>
 * <h2>Annotation processing</h2>
 * Generally, a manual implementation of {@link Writable} should not be required unless an object has a complex
 * definition. This interface can be automatically derived using the {@link ai.swim.structure.annotations.AutoForm}
 * annotation.
 * <p>
 * The annotation processor will derive an implementation of {@link Writable} for abstract and concrete classes and
 * interfaces. Any type parameters found will be lazily resolved when required and stored for further invocations of
 * the instance.
 * <p>
 * Like a derived {@link ai.swim.structure.recognizer.Recognizer} implementation, any wildcard types are unrolled such
 * that the bound replaces the wildcard but there is no untyped {@link Writable} for a wildcard with no bound.
 *
 * @param <F> the type this {@link Writable} transforms.
 */
public interface Writable<F> {

  /**
   * Transform's {@code from} into a new representation.
   *
   * @param from             the value to transform.
   * @param structuralWriter the interpreter to use in the transformation.
   * @param <T>              the type of the new representation.
   * @return the object's new representation.
   */
  <T> T writeInto(F from, StructuralWriter<T> structuralWriter);

  /**
   * Transforms {@code value} into a {@link Value} representation.
   */
  default Value asValue(F value) {
    return writeInto(value, new ValueStructuralWriter());
  }

  /**
   * Returns a Recon representation of {@code value} using the provided {@link PrintStrategy}.
   */
  default String print(F value, PrintStrategy printStrategy) {
    StringWriter stringWriter = new StringWriter();
    Recon.printRecon(stringWriter, this, value, printStrategy);
    return stringWriter.toString();
  }

  /**
   * Returns an inline Recon representation of {@code value}.
   */
  default String asReconString(F value) {
    return print(value, PrintStrategy.STANDARD);
  }

  /**
   * Returns a Recon representation of {@code value}.
   */
  default String asCompactReconString(F value) {
    return print(value, PrintStrategy.COMPACT);
  }

  /**
   * Returns a pretty Recon representation of {@code value}.
   */
  default String asPrettyReconString(F value) {
    return print(value, new PrettyPrintStrategy());
  }

}
