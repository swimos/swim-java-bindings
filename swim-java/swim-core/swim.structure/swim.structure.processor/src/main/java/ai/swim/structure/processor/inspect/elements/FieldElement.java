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

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.accessor.Accessor;
import ai.swim.structure.processor.models.ModelLookup;
import ai.swim.structure.processor.schema.FieldModel;

import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class FieldElement {

  private final Accessor accessor;
  private final VariableElement element;
  private final FieldKind fieldKind;

  public FieldElement(Accessor accessor,  VariableElement element, FieldKind fieldKind) {
    this.accessor = accessor;
    this.element = element;
    this.fieldKind = fieldKind;
  }

  public String propertyName() {
    AutoForm.Name name = this.element.getAnnotation(AutoForm.Name.class);
    if (name != null) {
      return name.value();
    } else {
      return this.element.getSimpleName().toString();
    }
  }

  public TypeMirror type() {
    return element.asType();
  }

  public boolean isOptional() {
    return this.element.getAnnotation(AutoForm.Optional.class) != null;
  }

  public boolean isIgnored() {
    return this.element.getAnnotation(AutoForm.Ignore.class) != null;
  }

  public Name getName() {
    return this.element.getSimpleName();
  }

  public VariableElement getElement() {
    return this.element;
  }

  public FieldModel transform(ModelLookup modelLookup, ScopedContext context) {
    return new FieldModel(
        accessor,
        modelLookup.lookup(element.asType(), context),
        element,
        fieldKind
    );
  }
}
