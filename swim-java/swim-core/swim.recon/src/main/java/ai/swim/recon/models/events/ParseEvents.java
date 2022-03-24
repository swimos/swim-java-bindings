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

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.stage.FinalAttr;

public abstract class ParseEvents {

  public static ParseEvents noEvent() {
    return new NoParseEvent();
  }

  public static ParseEvents singleEvent(ReadEvent event) {
    return new SingleParseEvent(event);
  }

  public static ParseEvents twoEvents(ReadEvent event1, ReadEvent event2) {
    return new TwoParseEvents(event1, event2);
  }

  public static ParseEvents threeEvents(ReadEvent event1, ReadEvent event2, ReadEvent event3) {
    return new ThreeParseEvents(event1, event2, event3);
  }

  public static ParseEvents terminateWithAttr(FinalAttr stage) {
    return new TerminateWithAttrParseEvent(stage);
  }

  public static ParseEvents end() {
    return new EndParseEvent();
  }

  public boolean isNoEvent() {
    return false;
  }

  public boolean isSingleEvent() {
    return false;
  }

  public boolean isTwoEvents() {
    return false;
  }

  public boolean isThreeEvents() {
    return false;
  }

  public boolean isTerminateWithAttr() {
    return false;
  }

  public boolean isEnd() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }

  public enum ParseState {
    Init,
    AttrBodyStartOrNl,
    AttrBodyAfterValue,
    AttrBodyAfterSlot,
    AttrBodySlot,
    AttrBodyAfterSep,
    AfterAttr,
    RecordBodyStartOrNl,
    RecordBodyAfterValue,
    RecordBodyAfterSlot,
    RecordBodySlot,
    RecordBodyAfterSep,
  }
}