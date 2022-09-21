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

package ai.swim.structure.processor.inspect;

import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.recognizer.context.ScopedMessager;

public class Manifest {
  boolean hasHeaderBody;
  boolean hasBody;
  boolean hasTag;//todo

  public boolean validate(FieldKind kind, ScopedMessager messager) {
    switch (kind) {
      case Body:
        if (hasBody) {
          messager.error("At most one field can replace the body.");
          return false;
        }
        hasBody = true;
        break;
      case HeaderBody:
        if (hasHeaderBody) {
          messager.error("At most one field can replace the tag body.");
          return false;
        }
        hasHeaderBody = true;
        break;
      default:
        break;
    }
    return true;
  }
}
