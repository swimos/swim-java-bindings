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
import ai.swim.recon.models.state.Action;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ParserTransition {
  private final List<ReadEvent> events;
  private final Action action;

  public ParserTransition(ReadEvent event, Action action) {
    this.events = List.of(event);
    this.action = Objects.requireNonNullElse(action, Action.none());
  }

  public ParserTransition() {
    this.events = Collections.emptyList();
    this.action = Action.none();
  }

  public ParserTransition(ReadEvent event) {
    this.events = List.of(event);
    this.action = Action.none();
  }

  public ParserTransition(ReadEvent event1, ReadEvent event2, Action action) {
    this.events = List.of(event1, event2);
    this.action = Objects.requireNonNullElse(action, Action.none());
  }

  public ParserTransition(List<ReadEvent> events, Action action) {
    this.events = events;
    this.action = action;
  }

  public List<ReadEvent> getEvents() {
    return events;
  }

  public Action getAction() {
    return action;
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
    return Objects.equals(events, that.events) && Objects.equals(action, that.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(events, action);
  }

  @Override
  public String toString() {
    return "ParserTransition{" +
        "events=" + events +
        ", change=" + action +
        '}';
  }
}
