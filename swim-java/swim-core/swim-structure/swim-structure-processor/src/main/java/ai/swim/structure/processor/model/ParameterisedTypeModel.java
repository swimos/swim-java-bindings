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
import java.util.Arrays;

/**
 * A model representing a known class type and for resolving known types; primitives, array types, list and map types.
 */
public class ParameterisedTypeModel extends Model {

  private final Mapping typeMapping;
  private final Model[] typeModels;

  public ParameterisedTypeModel(TypeMirror mirror,
      Element element,
      PackageElement packageElement,
      Mapping typeMapping,
      Model... typeModels) {
    super(mirror, element, packageElement);
    this.typeMapping = typeMapping;
    this.typeModels = typeModels;
  }

  @Override
  public boolean isParameterisedType() {
    return true;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.declared(this, inConstructor, typeModels);
  }

  /**
   * Returns the type that this known type model references.
   */
  public Mapping getTypeMapping() {
    return typeMapping;
  }

  @Override
  public String toString() {
    return "ParameterisedTypeModel{" +
        "typeMapping=" + typeMapping +
        ", typeModels=" + Arrays.toString(typeModels) +
        '}';
  }

  /**
   * An enumeration over {@link ParameterisedTypeModel} kinds for discrimination.
   */
  public enum Mapping {
    /**
     * java.util.List
     */
    List,
    /**
     * java.util.Map
     */
    Map
  }
}
