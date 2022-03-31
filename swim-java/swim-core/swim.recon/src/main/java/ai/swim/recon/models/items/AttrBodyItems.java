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
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.state.StateChange;

public class AttrBodyItems implements ItemsKind {
  AttrBodyItems() {

  }

  @Override
  public ParseEvents.ParseState startOrNl() {
    return ParseEvents.ParseState.AttrBodyStartOrNl;
  }

  @Override
  public ParseEvents.ParseState afterSep() {
    return ParseEvents.ParseState.AttrBodyAfterSep;
  }

  @Override
  public ParseEvents.ParseState startSlot() {
    return ParseEvents.ParseState.AttrBodySlot;
  }

  @Override
  public ParseEvents.ParseState afterValue() {
    return ParseEvents.ParseState.AttrBodyAfterValue;
  }

  @Override
  public ParseEvents.ParseState afterSlot() {
    return ParseEvents.ParseState.AttrBodyAfterSlot;
  }

  @Override
  public char endDelim() {
    return ')';
  }

  @Override
  public ReadEvent endEvent() {
    return ReadEvent.endAttribute();
  }

  @Override
  public StateChange endStateChange() {
    return StateChange.popAfterAttr();
  }
}
