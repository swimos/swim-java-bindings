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

package ai.swim.structure.processor.context;

import javax.lang.model.element.PackageElement;

public class NameFactory {
  private final String name;
  private final PackageElement packageElement;

  public NameFactory(String name, PackageElement packageElement) {
    this.name = name;
    this.packageElement = packageElement;
  }

  private static String replaceBounds(String from) {
    if (from.startsWith("<")) {
      from = from.replace("<", "");
    }
    if (from.endsWith(">")) {
      from = from.replace(">", "");
    }
    return from;
  }

  public String builderClassName() {
    return String.format("%sBuilder", this.name);
  }

  public String headerBuilderClassName() {
    return String.format("%sHeaderBuilder", this.name);
  }

  public String headerBuilderCanonicalName() {
    return String.format("%s.%s.%s", this.packageElement.getQualifiedName(), this.recognizerClassName(), this.headerBuilderClassName());
  }

  public String headerClassName() {
    return String.format("%sHeader", this.name);
  }

  public String headerCanonicalName() {
    return String.format("%s.%s.%sHeader", this.packageElement.getQualifiedName(), this.recognizerClassName(), this.name);
  }

  public String headerBuilderFieldName() {
    return "headerBuilder";
  }

  public String fieldBuilderName(String fieldName) {
    return String.format("%sBuilder", fieldName);
  }

  public String headerBuilderMethod() {
    return "ai.swim.structure.recognizer.structural.delegate.HeaderRecognizer.headerBuilder";
  }

  public String recognizerClassName() {
    return String.format("%sRecognizer", this.name);
  }

  public String writerClassName() {
    return String.format("%sWriter", this.name);
  }

  public String concreteRecognizerClassName() {
    return String.format("%sConcreteRecognizer", this.name);
  }

  public String typeParameterName(String gen) {
    return String.format("%sType", replaceBounds(gen).toLowerCase());
  }

  public String writableName(String gen) {
    return String.format("%sWritable", replaceBounds(gen).toLowerCase());
  }

  public String getName() {
    return name;
  }

  public String enumSpec() {
    return String.format("%Spec", this.name);
  }
}
