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

package ai.swim.recon.models.state;

import java.util.Objects;

public class PushAttrNewRec extends Action {

  private final boolean hasBody;

  public PushAttrNewRec(boolean hasBody) {
    this.hasBody = hasBody;
  }

  public boolean hasBody() {
    return hasBody;
  }

  @Override
  public boolean isPushAttrNewRec() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PushAttrNewRec that = (PushAttrNewRec) o;
    return hasBody == that.hasBody;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hasBody);
  }

  @Override
  public String toString() {
    return "PushAttrNewRec{" +
        "hasBody=" + hasBody +
        '}';
  }
}
