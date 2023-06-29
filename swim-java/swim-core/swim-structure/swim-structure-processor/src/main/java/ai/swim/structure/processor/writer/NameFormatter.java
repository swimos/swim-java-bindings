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

import javax.lang.model.element.PackageElement;

/**
 * A factory for producing identified components for use by the writer.
 */
public class NameFormatter {
  /**
   * The name of the class that triggered the processor.
   */
  protected final String name;
  /**
   * The package of the class that triggered the processor.
   */
  protected final PackageElement packageElement;

  public NameFormatter(String name, PackageElement packageElement) {
    this.name = name;
    this.packageElement = packageElement;
  }

  /**
   * Strips generic bounds from 'from'. I.e, strips '<' and '>' from "<G>"
   */
  protected static String replaceBounds(String from) {
    if (from.startsWith("<")) {
      from = from.replace("<", "");
    }
    if (from.endsWith(">")) {
      from = from.replace(">", "");
    }
    return from;
  }

  /**
   * Returns a type parameter representation of 'gen'.
   */
  public String typeParameterName(String gen) {
    return String.format("%sType", replaceBounds(gen).toLowerCase());
  }

  /**
   * Returns the name of the class that triggered the processor.
   */
  public String getName() {
    return name;
  }


}
