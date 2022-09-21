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

package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.inspect.elements.ClassElement;
import ai.swim.structure.processor.inspect.elements.InterfaceElement;
import ai.swim.structure.processor.inspect.elements.PrimitiveElement;
import ai.swim.structure.processor.inspect.elements.UnresolvedElement;
import ai.swim.structure.processor.inspect.elements.visitor.ElementVisitor;
import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.models.ClassMap;
import ai.swim.structure.processor.recognizer.models.InterfaceMap;
import ai.swim.structure.processor.recognizer.models.RecognizerModel;
import ai.swim.structure.processor.schema.FieldModel;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.stream.Collectors;

public class RecognizerProcessor implements ElementVisitor<RecognizerModel> {
  private final ScopedContext context;

  public RecognizerProcessor(ScopedContext context) {
    this.context = context;
  }

  @Override
  public RecognizerModel visitPrimitive(PrimitiveElement element) {
    return context.getRecognizerFactory().lookup(element.getType());
  }

  @Override
  public RecognizerModel visitClass(ClassElement element) {
    ClassMap classMap = new ClassMap(element.getRoot(), element.getDeclaredPackage());

    List<ExecutableElement> methods = classMap.getMethods();
    classMap.setMethods(methods);

    List<FieldModel> fieldModels = element.getFields().stream().map(field -> new FieldModel(
        field.getAccessor(),
        RecognizerModel.from(field.type(), context),
        field.getElement(),
        field.getFieldKind()
    )).collect(Collectors.toList());

    classMap.setFields(fieldModels);
    classMap.setSubTypes(element.getSubTypes().stream().map(subType -> subType.accept(this)).collect(Collectors.toList()));
    classMap.setAbstract(element.isAbstract());

    return classMap;
  }

  @Override
  public RecognizerModel visitInterface(InterfaceElement element) {
    return new InterfaceMap(
        element.getRoot(),
        element.getDeclaredPackage(),
        element.getSubTypes().stream().map(subType -> subType.accept(this)).collect(Collectors.toList())
    );
  }

  @Override
  public RecognizerModel visitUnresolved(UnresolvedElement element) {
    return RecognizerModel.runtimeLookup(element.type());
  }
}
