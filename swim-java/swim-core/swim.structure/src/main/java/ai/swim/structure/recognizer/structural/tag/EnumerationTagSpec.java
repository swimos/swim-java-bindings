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

package ai.swim.structure.recognizer.structural.tag;

import java.util.List;

public class EnumerationTagSpec extends TagSpec{
  private final List<String> tags;
  private String matched;

  public EnumerationTagSpec(List<String> tags) {
    this.tags = tags;
  }

  public boolean validate(String tag) {
    if (tags.contains(tag) ) {
      matched = tag;
      return true;
    }else {
      return false;
    }
  }

  public String getEnumVariant() {
    return matched;
  }

  @Override
  public boolean isEnumeration() {
    return true;
  }
}
