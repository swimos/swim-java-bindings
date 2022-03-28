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

import java.util.Objects;

public class TwoParseEvents extends ParseEvents {
  private final ReadEvent event1;
  private final ReadEvent event2;

  TwoParseEvents(ReadEvent event1, ReadEvent event2) {
    this.event1 = event1;
    this.event2 = event2;
  }

  public ReadEvent getEvent1() {
    return event1;
  }

  public ReadEvent getEvent2() {
    return event2;
  }

  @Override
  public boolean isTwoEvents() {
    return true;
  }

  @Override
  public String toString() {
    return "TwoParseEvents{" +
        "event1=" + event1 +
        ", event2=" + event2 +
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
    TwoParseEvents that = (TwoParseEvents) o;
    return Objects.equals(event1, that.event1) && Objects.equals(event2, that.event2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(event1, event2);
  }
}
