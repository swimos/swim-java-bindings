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

package ai.swim.structure.recognizer.structural.labelled;

public abstract class LabelledFieldKey {
  public static LabelledFieldKey TAG = new LabelledFieldKey() {
    @Override
    public boolean isTag() {
      return true;
    }
  };
  public static LabelledFieldKey HEADER = new LabelledFieldKey() {
    @Override
    public boolean isHeader() {
      return true;
    }
  };

  public static LabelledFieldKey attr(String name) {
    return new AttrFieldKey(name);
  }

  public static LabelledFieldKey item(String name) {
    return new ItemFieldKey(name);
  }

  public boolean isTag() {
    return false;
  }

  public boolean isHeader() {
    return false;
  }

  public boolean isAttribute() {
    return false;
  }

  public boolean isItem() {
    return false;
  }

  public static class AttrFieldKey extends LabelledFieldKey {
    private final String key;

    public AttrFieldKey(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    @Override
    public boolean isAttribute() {
      return true;
    }

    @Override
    public String toString() {
      return "AttrFieldKey{" +
          "key='" + key + '\'' +
          '}';
    }
  }

  public static class ItemFieldKey extends LabelledFieldKey {

    private final String name;

    public ItemFieldKey(String name) {
      this.name = name;
    }

    @Override
    public boolean isItem() {
      return true;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "ItemFieldKey{" +
          "name='" + name + '\'' +
          '}';
    }
  }
}
