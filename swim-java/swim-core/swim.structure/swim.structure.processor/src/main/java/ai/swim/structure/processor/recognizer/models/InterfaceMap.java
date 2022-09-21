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

package ai.swim.structure.processor.recognizer.models;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class InterfaceMap extends StructuralRecognizer {

  private final Element root;
  private final PackageElement declaredPackage;
  private final List<RecognizerModel> subTypes;

  public InterfaceMap(Element root, PackageElement declaredPackage, List<RecognizerModel> subTypes) {
    super(root.asType());
    this.root = root;
    this.declaredPackage = declaredPackage;
    this.subTypes = subTypes;
  }

  public String recognizerName() {
    return this.root.getSimpleName().toString() + "Recognizer";
  }

  public String canonicalRecognizerName() {
    return String.format("%s.%s", this.declaredPackage.getQualifiedName().toString(), this.recognizerName());
  }

  public List<RecognizerModel> getSubTypes() {
    return subTypes;
  }

  public PackageElement getDeclaredPackage() {
    return declaredPackage;
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return root.asType();
  }

  public Element root() {
    return root;
  }
}
