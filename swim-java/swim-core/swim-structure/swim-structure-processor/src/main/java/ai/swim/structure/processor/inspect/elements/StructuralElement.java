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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public abstract class StructuralElement extends JavaElement {
  protected final List<ExecutableElement> methods;
  protected final PackageElement declaredPackage;
  protected final TypeElement root;
  protected List<StructuralElement> subTypes;

  protected StructuralElement(TypeElement root, PackageElement declaredPackage) {
    this.root = root;
    this.declaredPackage = declaredPackage;
    this.subTypes = new ArrayList<>();
    this.methods = new ArrayList<>();
  }

  protected StructuralElement(TypeElement root, PackageElement declaredPackage, List<StructuralElement> subTypes) {
    this.root = root;
    this.declaredPackage = declaredPackage;
    this.subTypes = subTypes;
    this.methods = new ArrayList<>();
  }

  public PackageElement getDeclaredPackage() {
    return declaredPackage;
  }

  public String getJavaClassName() {
    return this.root.getSimpleName().toString();
  }

  public List<ExecutableElement> getMethods() {
    return methods;
  }

  public void addMethod(ExecutableElement method) {
    this.methods.add(method);
  }

  public TypeElement getRoot() {
    return root;
  }

  public TypeMirror type() {
    return root.asType();
  }

  public List<StructuralElement> getSubTypes() {
    return subTypes;
  }

  public void setSubTypes(List<StructuralElement> subTypes) {
    this.subTypes = subTypes;
  }

}
