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

package ai.swim.structure.processor.schema;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.FieldModel;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import java.util.List;

public abstract class AbstractSchema {
  protected final ClassLikeModel model;
  protected final PartitionedFields partitionedFields;

  protected AbstractSchema(ClassLikeModel model, PartitionedFields partitionedFields) {
    this.model = model;
    this.partitionedFields = partitionedFields;
  }

  public static ClassSchema forClass(ClassLikeModel model) {
    return new ClassSchema(model, PartitionedFields.buildFrom(model.getFields()));
  }

  public List<FieldModel> getFields() {
    return this.model.getFields();
  }

  public PartitionedFields getPartitionedFields() {
    return partitionedFields;
  }

  public List<? extends TypeParameterElement> getTypeParameters() {
    return this.model.getElement().getTypeParameters();
  }

  public boolean isEnum() {
    return model.getElement().getKind() == ElementKind.ENUM;
  }

  public Element getRoot() {
    return model.getElement();
  }

  public String getTag() {
    AutoForm.Tag tag = getRoot().getAnnotation(AutoForm.Tag.class);

    if (tag == null || tag.value().isBlank()) {
      return model.getJavaClassName();
    } else {
      return tag.value();
    }
  }

  public ClassLikeModel getModel() {
    return model;
  }
}
