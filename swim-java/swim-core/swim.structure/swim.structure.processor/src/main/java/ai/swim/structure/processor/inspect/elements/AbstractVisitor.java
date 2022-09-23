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

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.elements.visitor.ElementVisitor;
import ai.swim.structure.processor.models.ClassMap;
import ai.swim.structure.processor.models.InterfaceMap;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.ModelLookup;
import ai.swim.structure.processor.recognizer.RecognizerClassMap;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractVisitor implements ElementVisitor {
  protected final ScopedContext context;
  private final ModelLookup modelLookup;

  public AbstractVisitor(ScopedContext context, ModelLookup modelLookup) {
    this.context = context;
    this.modelLookup = modelLookup;
  }

  @Override
  public Model visitClass(ClassElement element) {
    ClassMap classMap = new RecognizerClassMap(element.getRoot(), element.getDeclaredPackage());

    List<ExecutableElement> methods = classMap.getMethods();
    classMap.setMethods(methods);


    classMap.setFields(element.getFields().stream().map(f -> f.transform(modelLookup, context)).collect(Collectors.toList()));
    classMap.setSubTypes(element.getSubTypes().stream().map(subType -> subType.accept(this)).collect(Collectors.toList()));
    classMap.setAbstract(element.isAbstract());

    return classMap;
  }

  @Override
  public Model visitInterface(InterfaceElement element) {
    return new InterfaceMap(
        element.getRoot(),
        element.getDeclaredPackage(),
        element.getSubTypes().stream().map(subType -> subType.accept(this)).collect(Collectors.toList())
    );
  }

}
