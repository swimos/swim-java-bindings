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

package ai.swim.recon.event;

import java.util.Objects;

public class ReadNumberValue extends ReadEvent {

  // todo wrap class with a field denoting the type of the number
  private final Number value;

  public ReadNumberValue(Number value) {
    this.value = value;
  }

  @Override
  public boolean isNumber() {
    return true;
  }

  public Number value() {
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
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReadNumberValue that = (ReadNumberValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
