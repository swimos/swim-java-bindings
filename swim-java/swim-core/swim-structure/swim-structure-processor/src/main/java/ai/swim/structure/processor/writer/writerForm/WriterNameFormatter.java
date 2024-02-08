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

package ai.swim.structure.processor.writer.writerForm;


import ai.swim.structure.processor.writer.NameFormatter;
import javax.lang.model.element.PackageElement;

public class WriterNameFormatter extends NameFormatter {
  public WriterNameFormatter(String name, PackageElement packageElement) {
    super(name, packageElement);
  }

  /**
   * Returns a string with "Writable" suffixed.
   */
  public String writableName(String gen) {
    return String.format("%sWritable", replaceBounds(gen).toLowerCase());
  }

  /**
   * Returns a string with "Writer" suffixed.
   */
  public String writerClassName() {
    return String.format("%sWriter", this.name);
  }

  public String writerClassName(CharSequence name) {
    return String.format("%sWriter", name);
  }

}
