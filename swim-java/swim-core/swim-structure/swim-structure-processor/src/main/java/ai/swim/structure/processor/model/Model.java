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
 * A base model representation of a Java element. This may be a primitive type, array type or structural type
 * (class-like or an interface).
 * <p>
 * This class provides an {@code instantiate} method for applying a transformation between this model and some other
 * representation, such as a recognizer or a writer.
 */
public abstract class Model {
  /**
   * The type that this model represents.
   */
  protected final TypeMirror type;
  /**
   * The root element that this model was derived from.
   */
  protected final Element element;
  /**
   * The package element of this model's type.
   */
  protected final PackageElement packageElement;

  public Model(TypeMirror type, Element element, PackageElement packageElement) {
    this.type = type;
    this.element = element;
    this.packageElement = packageElement;
  }

  /**
   * Gets type that this model represents.
   */
  public TypeMirror getType() {
    return type;
  }

  /**
   * Gets the root element that this model was derived from.
   */
  public Element getElement() {
    return element;
  }

  /**
   * Gets the package element of this model's type.
   */
  public PackageElement getDeclaredPackage() {
    return packageElement;
  }

  /**
   * Returns whether this model is class-like.
   */
  public boolean isClassLike() {
    return false;
  }

  /**
   * Returns whether this model represents an unresolved type.
   * <p>
   * A model may be unresolved if it has been registered for runtime resolution.
   */
  public boolean isUnresolved() {
    return false;
  }

  /**
   * Returns whether this model represents a core library type.
   */
  public boolean isParameterisedType() {
    return false;
  }

  /**
   * Perform a transformation on this model and return an initialized type that contains its mapped {@code TypeMirror}
   * and a code block.
   *
   * @param initializer   a {@link TypeInitializer} for use during the transformation.
   * @param inConstructor whether the current block of code will be written out into a class constructor.
   * @return a mapped, initialized, type.
   */
  public abstract InitializedType instantiate(TypeInitializer initializer, boolean inConstructor);

  /**
   * Returns this model's default value. For an object, this may be null and for a primitive type it will be its default
   * value.
   */
  public Object defaultValue() {
    return null;
  }
}
