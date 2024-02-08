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

package ai.swim.structure.recognizer.structural.delegate;

public abstract class OrdinalFieldKey {

  public static final OrdinalFieldKey TAG = new OrdinalFieldKey() {
    @Override
    public boolean isTag() {
      return true;
    }
  };
  public static final OrdinalFieldKey HEADER = new OrdinalFieldKey() {
    @Override
    public boolean isHeader() {
      return true;
    }
  };
  public static final OrdinalFieldKey FIRST_ITEM = new OrdinalFieldKey() {
    @Override
    public boolean isFirstItem() {
      return true;
    }
  };

  public static OrdinalFieldKey attr(String name) {
    return new OrdinalFieldKeyAttr(name);
  }

  public boolean isHeader() {
    return false;
  }

  public boolean isFirstItem() {
    return false;
  }

  public boolean isAttr() {
    return false;
  }

  public boolean isTag() {
    return false;
  }

  public static class OrdinalFieldKeyAttr extends OrdinalFieldKey {
    private final String name;

    public OrdinalFieldKeyAttr(String name) {
      this.name = name;
    }

    @Override
    public boolean isAttr() {
      return true;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "OrdinalFieldKeyAttr{" +
          "name='" + name + '\'' +
          '}';
    }
  }
}
