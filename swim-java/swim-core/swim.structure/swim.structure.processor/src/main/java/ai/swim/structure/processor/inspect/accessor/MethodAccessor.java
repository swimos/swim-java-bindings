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

package ai.swim.structure.processor.inspect.accessor;

import com.squareup.javapoet.CodeBlock;

import javax.lang.model.element.ExecutableElement;

public class MethodAccessor extends Accessor {
  private final ExecutableElement setMethod;
  private final ExecutableElement getMethod;

  public MethodAccessor(ExecutableElement setMethod, ExecutableElement getMethod) {
    this.setMethod = setMethod;
    this.getMethod = getMethod;
  }

  @Override
  public void writeSet(CodeBlock.Builder builder, String instance, Object arg) {
    builder.add("$L.$L($L);\n", instance, this.setMethod.getSimpleName(), arg);
  }

  @Override
  public void writeGet(CodeBlock.Builder builder, String instance) {
    builder.add("$L.$L()", instance, this.getMethod.getSimpleName());
  }

  @Override
  public String toString() {
    return "MethodAccessor{" +
        "setMethod=" + setMethod +
        "getMethod=" + getMethod +
        '}';
  }
}
