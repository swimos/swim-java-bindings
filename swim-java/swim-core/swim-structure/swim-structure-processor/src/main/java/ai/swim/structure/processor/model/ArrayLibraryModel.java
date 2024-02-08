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
 * An array type model. I.e, Integer[].
 */
public class ArrayLibraryModel extends Model {
  private final TypeMirror componentType;
  private final Model componentModel;

  /**
   * Constructs a array library model.
   *
   * @param type           the type of the array. I.e, Integer[]
   * @param element        the root element of this model.
   * @param componentType  the type of the array's elements. I.e, Integer
   * @param componentModel a model for the array's elements.
   * @param packageElement of the component type. I.e, java.lang.Integer.
   */
  public ArrayLibraryModel(TypeMirror type,
      Element element,
      TypeMirror componentType,
      Model componentModel,
      PackageElement packageElement) {
    super(type, element, packageElement);
    this.componentType = componentType;
    this.componentModel = componentModel;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.arrayType(this, inConstructor);
  }

  /**
   * Returns a model for the array's elements.
   */
  public Model getComponentModel() {
    return componentModel;
  }

  @Override
  public String toString() {
    return "ArrayLibraryModel{" +
        "arrayType=" + componentType +
        ", componentModel=" + componentModel +
        ", type=" + type +
        '}';
  }
}
