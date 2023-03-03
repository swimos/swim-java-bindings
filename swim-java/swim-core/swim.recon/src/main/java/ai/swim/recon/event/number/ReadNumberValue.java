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

package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;

import java.util.Objects;

public abstract class ReadNumberValue<N> extends ReadEvent {

  protected final N value;

  public ReadNumberValue(N value) {
    this.value = value;
  }

  public N value() {
    return this.value;
  }

  @Override
  public String toString() {
    return "ReadNumberValue{" +
        "value=" + value +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadNumberValue)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ReadNumberValue<?> that = (ReadNumberValue<?>) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

}
