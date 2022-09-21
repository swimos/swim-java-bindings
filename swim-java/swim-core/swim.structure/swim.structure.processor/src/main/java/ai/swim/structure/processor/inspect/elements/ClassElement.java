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

package ai.swim.structure.processor.inspect.elements;

import ai.swim.structure.processor.inspect.elements.visitor.ElementVisitor;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class ClassElement extends StructuralElement {
  private final List<FieldElement> fieldElements;

  private boolean isAbstract;

  public ClassElement(TypeElement root, PackageElement declaredPackage) {
    super(root, declaredPackage);
    this.fieldElements = new ArrayList<>();
  }

  @Override
  public <T> T accept(ElementVisitor<T> visitor) {
    return visitor.visitClass(this);
  }

  public FieldElement getFieldViewByPropertyName(String propertyName) {
    for (FieldElement element : this.fieldElements) {
      if (element.propertyName().equals(propertyName)) {
        return element;
      }
    }

    return null;
  }

  public List<FieldElement> getFields() {
    return fieldElements;
  }

  public void merge(ClassElement with) {
    this.methods.addAll(with.methods);
    this.fieldElements.addAll(with.fieldElements);
  }

  public void addField(FieldElement field) {
    this.fieldElements.add(field);
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public boolean isAbstract() {
    return isAbstract;
  }
}
