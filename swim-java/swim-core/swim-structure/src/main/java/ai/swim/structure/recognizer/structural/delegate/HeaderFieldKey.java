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

public abstract class HeaderFieldKey {
  public static final HeaderFieldKey HEADER_BODY = new HeaderFieldKey() {
    @Override
    public boolean isHeaderBody() {
      return true;
    }
  };

  public static HeaderFieldKey slot(String name) {
    return new HeaderSlotKey(name);
  }

  public boolean isHeaderBody() {
    return false;
  }

  public boolean isHeaderSlot() {
    return false;
  }

  public static class HeaderSlotKey extends HeaderFieldKey {

    private final String name;

    public HeaderSlotKey(String name) {
      this.name = name;
    }

    @Override
    public boolean isHeaderSlot() {
      return true;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "HeaderSlotKey{" +
          "name='" + name + '\'' +
          '}';
    }
  }
}
