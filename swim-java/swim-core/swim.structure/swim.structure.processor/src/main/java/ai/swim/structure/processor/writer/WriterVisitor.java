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

package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.elements.AbstractVisitor;
import ai.swim.structure.processor.inspect.elements.PrimitiveElement;
import ai.swim.structure.processor.inspect.elements.UnresolvedElement;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.RuntimeLookupModel;

public class WriterVisitor extends AbstractVisitor {
  public WriterVisitor(ScopedContext context) {
    super(context, new WriterModelLookup());
  }

  @Override
  public Model visitPrimitive(PrimitiveElement element) {
    return context.getWriterFactory().lookup(element.getType());
  }

  @Override
  public Model visitUnresolved(UnresolvedElement element) {
    return new RuntimeLookupModel(WriterModel.RUNTIME_LOOKUP, element.type());
  }
}
