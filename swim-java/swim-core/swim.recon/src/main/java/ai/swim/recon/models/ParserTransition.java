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

package ai.swim.recon.models;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.state.NoStateChange;
import ai.swim.recon.models.state.StateChange;

import java.util.Objects;

public class ParserTransition {
  private final ParseEvents events;
  private final StateChange change;

  public ParserTransition(ReadEvent event, StateChange change) {
    this.events = ParseEvents.singleEvent(event);
    this.change = Objects.requireNonNullElse(change, new NoStateChange());
  }

  public ParserTransition(ReadEvent event1, ReadEvent event2, StateChange change) {
    this.events = ParseEvents.twoEvents(event1, event2);
    this.change = Objects.requireNonNullElse(change, new NoStateChange());
  }

  public ParserTransition(ReadEvent event1, ReadEvent event2, ReadEvent event3, StateChange change) {
    this.events = ParseEvents.threeEvents(event1, event2, event3);
    this.change = Objects.requireNonNullElse(change, new NoStateChange());
  }

  public ParserTransition(ParseEvents events, StateChange change){
    this.events = events;
    this.change = change;
  }

  public ParseEvents getEvents() {
    return events;
  }

  public StateChange getChange() {
    return change;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParserTransition that = (ParserTransition) o;
    return Objects.equals(events, that.events) && Objects.equals(change, that.change);
  }

  @Override
  public int hashCode() {
    return Objects.hash(events, change);
  }

  @Override
  public String toString() {
    return "ParserTransition{" +
        "events=" + events +
        ", change=" + change +
        '}';
  }
}
