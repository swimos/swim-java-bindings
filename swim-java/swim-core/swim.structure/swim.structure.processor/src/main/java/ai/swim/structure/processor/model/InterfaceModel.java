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

package ai.swim.structure.processor.model;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.writer.Writer;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;

public class InterfaceModel extends StructuralModel {
  private final TypeElement root;

  public InterfaceModel(TypeElement root, PackageElement declaredPackage) {
    super(root.asType(), root, declaredPackage);
    this.root = root;
    this.subTypes = new ArrayList<>();
  }

  @Override
  public TypeElement getElement() {
    return root;
  }

  @Override
  public void write(Writer writer) throws IOException {
    writer.writeInterface(this);
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.declared(this, inConstructor);
  }

  public String getJavaClassName() {
    return this.root.getSimpleName().toString();
  }

  public String getTag() {
    AutoForm.Tag tag = this.root.getAnnotation(AutoForm.Tag.class);

    if (tag == null || tag.value().isBlank()) {
      return getJavaClassName();
    } else {
      return tag.value();
    }
  }

  @Override
  public String toString() {
    return "InterfaceModel{" +
            "root=" + root +
            ", subTypes=" + subTypes +
            ", declaredPackage=" + getDeclaredPackage() +
            ", type=" + type +
            '}';
  }
}
