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

package ai.swim.structure.write.proxy;

import ai.swim.structure.TypeParameter;
import ai.swim.structure.write.Writable;

import java.util.function.Supplier;

public abstract class WriterTypeParameter<T> extends TypeParameter<Writable<T>> {
  public static <T> WriterTypeParameter<T> forClass(Class<T> target) {
    return forSupplier(() -> WriterProxy.getProxy().lookup(target));
  }

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