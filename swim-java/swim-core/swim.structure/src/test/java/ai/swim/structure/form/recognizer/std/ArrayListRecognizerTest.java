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
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayListRecognizerTest {

  public static List<ReadEvent> listReadEvents(int base, int n) {
    List<ReadEvent> events = new ArrayList<>();
    events.add(ReadEvent.startBody());

    for (int i = 0; i < n; i++) {
      events.add(ReadEvent.number(base));
      base+=1;
    }

    events.add(ReadEvent.endRecord());

    return events;
  }

  @Test
  void recognizes() throws Exception {
    Recognizer<ArrayList<Integer>> recognizer = new ArrayListRecognizer<>(false, Integer.class);
    List<ReadEvent> events = listReadEvents(0,3);

    ArrayList<Integer> actual = RecognizerTestUtil.runTest(recognizer, events);
    List<Integer> expected = List.of(1, 2, 3);

    assertEquals(actual, expected);
  }

}