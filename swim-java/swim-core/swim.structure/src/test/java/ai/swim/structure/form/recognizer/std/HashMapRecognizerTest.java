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

package ai.swim.structure.form.recognizer.std;

import ai.swim.structure.form.RecognizerTestUtil;
import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HashMapRecognizerTest {

  public List<ReadEvent> hashMapReadEvents() {
    List<ReadEvent> events = new ArrayList<>();
    events.add(ReadEvent.startBody());
    events.add(ReadEvent.number(1));
    events.add(ReadEvent.slot());
    events.add(ReadEvent.text("1"));
    events.add(ReadEvent.number(2));
    events.add(ReadEvent.slot());
    events.add(ReadEvent.text("2"));
    events.add(ReadEvent.number(3));
    events.add(ReadEvent.slot());
    events.add(ReadEvent.text("3"));
    events.add(ReadEvent.endRecord());

    return events;
  }

  @Test
  void recognizes() throws Exception {
    Recognizer<HashMap<Integer, String>> recognizer = new HashMapRecognizer<>(Integer.class, String.class, false);
    List<ReadEvent> events = hashMapReadEvents();

    HashMap<Integer, String> actual = RecognizerTestUtil.runTest(recognizer, events);
    Map<Integer, String> expected = Map.ofEntries(
        Map.entry(1,"1"),
        Map.entry(2,"2"),
        Map.entry(3,"3")
    );

    assertEquals(actual, expected);
  }
}
