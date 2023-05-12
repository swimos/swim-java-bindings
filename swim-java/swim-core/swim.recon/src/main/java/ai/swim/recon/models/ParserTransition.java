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
import ai.swim.recon.models.state.StateChange;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ParserTransition {
  private final List<ReadEvent> events;
  private final StateChange change;

  public ParserTransition(ReadEvent event, StateChange change) {
    this.events = List.of(event);
    this.change = Objects.requireNonNullElse(change, StateChange.none());
  }

  public ParserTransition() {
    this.events = Collections.emptyList();
    this.change = StateChange.none();
  }

  public ParserTransition(ReadEvent event) {
    this.events = List.of(event);
    this.change = StateChange.none();
  }

  public ParserTransition(ReadEvent event1, ReadEvent event2, StateChange change) {
    this.events = List.of(event1, event2);
    this.change = Objects.requireNonNullElse(change, StateChange.none());
  }

  public ParserTransition(List<ReadEvent> events, StateChange change) {
    this.events = events;
    this.change = change;
  }

  public List<ReadEvent> getEvents() {
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
    if (!(o instanceof ParserTransition)) {
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
