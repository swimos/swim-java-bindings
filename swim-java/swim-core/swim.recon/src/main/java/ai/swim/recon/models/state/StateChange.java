// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.recon.models.state;

public abstract class StateChange {
  private static StateChange POP_AFTER_ATTR;
  private static StateChange POP_AFTER_ITEM;
  private static StateChange PUSH_ATTR;
  private static StateChange PUSH_BODY;
  private static StateChange NONE;

  public static StateChange popAfterAttr() {
    if (POP_AFTER_ATTR == null) {
      POP_AFTER_ATTR = new PopAfterAttr();
    }
    return POP_AFTER_ATTR;
  }

  public static StateChange popAfterItem() {
    if (POP_AFTER_ITEM == null) {
      POP_AFTER_ITEM = new PopAfterItem();
    }
    return POP_AFTER_ITEM;
  }

  public static StateChange none() {
    if (NONE == null) {
      NONE = new NoStateChange();
    }
    return NONE;
  }

  public static StateChange pushAttr() {
    if (PUSH_ATTR == null) {
      PUSH_ATTR = new PushAttr();
    }
    return PUSH_ATTR;
  }

  public static StateChange pushBody() {
    if (PUSH_BODY == null) {
      PUSH_BODY = new PushBody();
    }
    return PUSH_BODY;
  }

  public boolean isNone() {
    return false;
  }

  public boolean isPopAfterAttr() {
    return false;
  }

  public boolean isPopAfterItem() {
    return false;
  }

  public boolean isChangeState() {
    return false;
  }

  public boolean isPushAttr() {
    return false;
  }

  public boolean isPushAttrNewRec() {
    return false;
  }

  public boolean isPushBody() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }
}
