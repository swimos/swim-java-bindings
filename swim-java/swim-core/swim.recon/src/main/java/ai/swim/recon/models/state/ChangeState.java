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

import ai.swim.recon.models.ParseState;

import java.util.Objects;

public class ChangeState extends StateChange {

  private final ParseState state;

  public ChangeState(ParseState state) {
    this.state = state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChangeState that = (ChangeState) o;
    return state == that.state;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state);
  }

  public ParseState getState() {
    return state;
  }

  @Override
  public boolean isChangeState() {
    return true;
  }

  @Override
  public String toString() {
    return "ChangeState{" +
        "state=" + state +
        '}';
  }
}
