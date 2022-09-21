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

package ai.swim.structure.processor.recognizer.context;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class ScopedMessager {
  private final Messager messager;
  private final Element root;

  public ScopedMessager(Messager messager, Element root) {
    this.messager = messager;
    this.root = root;
  }

  public void log(Diagnostic.Kind kind, String event) {
    String message = String.format("%s: %s", this.root, event);
    this.messager.printMessage(kind, message);
  }

  public void error(String event) {
    this.log(Diagnostic.Kind.ERROR, event);
  }
}
