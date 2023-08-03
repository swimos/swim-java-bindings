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

package ai.swim.structure.writer.proxy;

import ai.swim.structure.TypeParameter;
import ai.swim.structure.writer.Writable;
import java.util.function.Supplier;

/**
 * A type parameter which can be used to initialise a {@code Writable} that has generic type parameters. Writer type
 * parameters are safe to be reused.
 *
 * @param <T> the type the {@code Writable} produces.
 */
public abstract class WriterTypeParameter<T> extends TypeParameter<Writable<T>> {
  /**
   * Builds a new type parameter that will perform a lookup for {@code T} using the writer proxy.
   *
   * @param target class to lookup.
   * @param <T>    the type the {@code Writable} produces.
   * @return a new type parameter.
   */
  public static <T> WriterTypeParameter<T> forClass(Class<T> target) {
    return forSupplier(() -> WriterProxy.getProxy().lookup(target));
  }

  /**
   * Builds a new type parameter that will invoke the provided supplier when the {@code Writable<T>} is required.
   *
   * @param supplier that returns a {@code Writable<T>}
   * @param <T>      the type the {@code Writable} produces.
   * @return a new type parameter.
   */
  private static <T> WriterTypeParameter<T> forSupplier(Supplier<Writable<T>> supplier) {
    return new SupplierParameter<>(supplier);
  }
}

final class SupplierParameter<T> extends WriterTypeParameter<T> {
  private final Supplier<Writable<T>> supplier;

  SupplierParameter(Supplier<Writable<T>> supplier) {
    this.supplier = supplier;
  }

  @Override
  public Writable<T> build() {
    return this.supplier.get();
  }
}