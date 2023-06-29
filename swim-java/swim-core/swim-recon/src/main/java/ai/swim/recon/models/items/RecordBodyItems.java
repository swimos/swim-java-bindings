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

package ai.swim.recon.models.items;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.ParseState;
import ai.swim.recon.models.state.Action;

public class RecordBodyItems implements ItemsKind {
  RecordBodyItems() {

  }

  @Override
  public ParseState startOrNl() {
    return ParseState.RecordBodyStartOrNl;
  }

  @Override
  public ParseState afterSep() {
    return ParseState.RecordBodyAfterSep;
  }

  @Override
  public ParseState startSlot() {
    return ParseState.RecordBodySlot;
  }

  @Override
  public ParseState afterValue() {
    return ParseState.RecordBodyAfterValue;
  }

  @Override
  public ParseState afterSlot() {
    return ParseState.RecordBodyAfterSlot;
  }

  @Override
  public char endDelim() {
    return '}';
  }

  @Override
  public ReadEvent endEvent() {
    return ReadEvent.endRecord();
  }

  @Override
  public Action endStateChange() {
    return Action.popAfterItem();
  }
}
