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
import ai.swim.structure.processor.models.Model;

import javax.lang.model.type.TypeMirror;

public class PrimitiveElement extends JavaElement {
  private final TypeMirror type;

  private PrimitiveElement(TypeMirror type) {
    this.type = type;
  }

  @Override
  public Model accept(ElementVisitor visitor) {
    return visitor.visitPrimitive(this);
  }

  public TypeMirror getType() {
    return type;
  }
}
