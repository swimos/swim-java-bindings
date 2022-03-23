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

package ai.swim.recon.models.events;

import ai.swim.recon.models.stage.FinalAttr;
import java.util.Objects;

public class TerminateWithAttrParseEvent extends ParseEvents {

  private final FinalAttr stage;

  TerminateWithAttrParseEvent(FinalAttr stage){
    this.stage = stage;
  }


  public FinalAttr getStage() {
    return stage;
  }

  @Override
  public boolean isTerminateWithAttr() {
    return true;
  }

  @Override
  public String toString() {
    return "TerminateWithAttrParseEvent{" +
        "stage=" + stage +
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
    TerminateWithAttrParseEvent that = (TerminateWithAttrParseEvent) o;
    return Objects.equals(stage, that.stage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stage);
  }
}
