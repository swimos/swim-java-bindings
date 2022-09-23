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

package ai.swim.structure.processor.models;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public abstract class ClassMap extends StructuralModel {

  protected final TypeElement root;
  protected final PackageElement declaredPackage;
  protected List<FieldModel> memberVariables;
  protected List<ExecutableElement> methods;
  protected List<Model> subTypes;
  protected boolean isAbstract;

  public ClassMap(TypeElement root, PackageElement declaredPackage) {
    super(root.asType());
    this.root = root;
    this.memberVariables = new ArrayList<>();
    this.methods = new ArrayList<>();
    this.declaredPackage = declaredPackage;
    this.subTypes = new ArrayList<>();
  }

  public abstract String concreteName();

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    return CodeBlock.of("new $L()", this.concreteName());
  }

  public PackageElement getDeclaredPackage() {
    return declaredPackage;
  }

  public String getJavaClassName() {
    return this.root.getSimpleName().toString();
  }

  public String getTag() {
    AutoForm autoForm = this.root.getAnnotation(AutoForm.class);

    if (autoForm.value().isBlank()) {
      return getJavaClassName();
    } else {
      return autoForm.value();
    }
  }

  public List<ExecutableElement> getMethods() {
    return methods;
  }

  public void setMethods(List<ExecutableElement> methods) {
    this.methods = methods;
  }

  @Override
  public String toString() {
    return "ClassMap{" +
        "root=" + root +
        ", memberVariables=" + memberVariables +
        ", methods=" + methods +
        ", declaredPackage=" + declaredPackage +
        ", subTypes=" + subTypes +
        '}';
  }

  public TypeElement getRoot() {
    return root;
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return root.asType();
  }

  public List<FieldModel> getFieldModels() {
    return this.memberVariables;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public List<Model> getSubTypes() {
    return subTypes;
  }

  public void setSubTypes(List<Model> subTypes) {
    this.subTypes = subTypes;
  }

  public void setFields(List<FieldModel> fieldModels) {
    this.memberVariables = fieldModels;
  }

  @Override
  public boolean isClass() {
    return true;
  }
}
