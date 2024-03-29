/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

/**
 * A model that represents a generic type parameter that had no bounds placed on it, such as {@code E}.
 */
public class UntypedModel extends Model {
  public UntypedModel(TypeMirror typeMirror, Element element, PackageElement packageElement) {
    super(typeMirror, element, packageElement);
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) throws InvalidModelException {
    return initializer.untyped(type, inConstructor);
  }

  @Override
  public String toString() {
    return "UntypedModel{" +
        "type=" + type +
        '}';
  }
}
