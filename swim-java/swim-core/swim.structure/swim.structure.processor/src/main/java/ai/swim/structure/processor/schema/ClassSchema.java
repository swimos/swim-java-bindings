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
import ai.swim.structure.processor.models.ClassMap;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.List;
import java.util.stream.Collectors;

public class ClassSchema {
  private final ClassMap classMap;
  private final PartitionedFields partitionedFields;

  private ClassSchema(ClassMap classMap, PartitionedFields partitionedFields) {
    this.classMap = classMap;
    this.partitionedFields = partitionedFields;
  }

  public static ClassSchema fromMap(ClassMap classMap) {
    return new ClassSchema(classMap, PartitionedFields.buildFrom(classMap.getFieldModels()));
  }

  public PackageElement getDeclaredPackage() {
    return this.classMap.getDeclaredPackage();
  }

  public PartitionedFields getPartitionedFields() {
    return partitionedFields;
  }

  public List<FieldDiscriminate> discriminate() {
    return this.partitionedFields.discriminate();
  }

  @Override
  public String toString() {
    return "ClassSchema{" +
        "classMap=" + classMap +
        ", partitionedFields=" + partitionedFields +
        '}';
  }

  public String getTag() {
    if (!classMap.isClass()) {
      throw new IllegalStateException(String.format("%s is not a class", classMap.getRoot()));
    } else {
      return this.classMap.getTag();
    }
  }

  public List<String> getVariantTags() {
    if (!classMap.isEnum()) {
      throw new IllegalStateException(String.format("%s is not an enum", classMap.getRoot()));
    } else {
      return classMap
          .getRoot()
          .getEnclosedElements()
          .stream()
          .filter(e -> e.getKind().equals(ElementKind.ENUM_CONSTANT))
          .map(e -> {
            AutoForm.Tag tag = e.getAnnotation(AutoForm.Tag.class);
            if (tag != null && !tag.value().isBlank()) {
              return tag.value();
            } else {
              return e.toString();
            }
          })
          .collect(Collectors.toList());
    }
  }

  public ClassMap getClassMap() {
    return classMap;
  }

  public Name qualifiedName() {
    return this.classMap.getRoot().getQualifiedName();
  }

  public List<? extends TypeParameterElement> getTypeParameters() {
    return this.classMap.getRoot().getTypeParameters();
  }

}
