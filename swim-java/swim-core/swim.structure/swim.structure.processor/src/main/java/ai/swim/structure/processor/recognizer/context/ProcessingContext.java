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

import ai.swim.structure.processor.recognizer.models.RecognizerFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public class ProcessingContext {

  private final ProcessingEnvironment processingEnvironment;
  private final RecognizerFactory factory;

  public ProcessingContext(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
    this.factory = RecognizerFactory.initFrom(processingEnvironment);
  }

  public ProcessingEnvironment getProcessingEnvironment() {
    return processingEnvironment;
  }

  public RecognizerFactory getFactory() {
    return factory;
  }

  @Override
  public String toString() {
    return "ProcessingContext{" +
        "processingEnvironment=" + processingEnvironment +
        '}';
  }


}
