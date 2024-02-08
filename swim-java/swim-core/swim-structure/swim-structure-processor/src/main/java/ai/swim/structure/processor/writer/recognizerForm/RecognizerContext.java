/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.structure.processor.writer.recognizerForm;

import ai.swim.structure.processor.model.ModelInspector;
import ai.swim.structure.processor.writer.Context;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

public class RecognizerContext extends Context<RecognizerTypeInitializer, RecognizerNameFormatter> {
  private RecognizerContext(Element root,
      ProcessingEnvironment processingEnvironment,
      String name,
      PackageElement packageElement,
      RecognizerTypeInitializer initializer) {
    super(root, processingEnvironment, initializer, new RecognizerNameFormatter(name, packageElement));
  }

  public static RecognizerContext build(Element root,
      ProcessingEnvironment processingEnvironment,
      ModelInspector inspector,
      String name,
      PackageElement packageElement) {
    RecognizerNameFormatter formatter = new RecognizerNameFormatter(name, packageElement);
    RecognizerTypeInitializer initializer = new RecognizerTypeInitializer(processingEnvironment, formatter, inspector);
    return new RecognizerContext(root, processingEnvironment, name, packageElement, initializer);
  }
}
