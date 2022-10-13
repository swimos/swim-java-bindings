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
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.ModelLookup;
import ai.swim.structure.processor.models.RuntimeLookupModel;

import javax.lang.model.type.TypeMirror;

public class WriterModelLookup implements ModelLookup {
  @Override
  public Model lookup(TypeMirror typeMirror, ScopedContext context) {
    return WriterModel.from(typeMirror, context);
  }

  @Override
  public Model untyped(TypeMirror type) {
    return new RuntimeLookupModel(WriterModel.RUNTIME_LOOKUP, type);
  }
}
