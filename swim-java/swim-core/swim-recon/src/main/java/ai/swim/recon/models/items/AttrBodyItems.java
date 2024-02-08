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

package ai.swim.recon.models.items;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.ParseState;
import ai.swim.recon.models.state.Action;

public class AttrBodyItems implements ItemsKind {
  AttrBodyItems() {

  }

  @Override
  public ParseState startOrNl() {
    return ParseState.AttrBodyStartOrNl;
  }

  @Override
  public ParseState afterSep() {
    return ParseState.AttrBodyAfterSep;
  }

  @Override
  public ParseState startSlot() {
    return ParseState.AttrBodySlot;
  }

  @Override
  public ParseState afterValue() {
    return ParseState.AttrBodyAfterValue;
  }

  @Override
  public ParseState afterSlot() {
    return ParseState.AttrBodyAfterSlot;
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
  public Action endStateChange() {
    return Action.popAfterAttr();
  }
}
