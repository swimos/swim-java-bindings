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
import ai.swim.structure.form.recognizer.RecognizerProxy;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexRecognizerTest {

//  @Test
  void complexDataStructure() throws Exception {
    Map<Integer, List<Integer>> expected = Map.ofEntries(
        Map.entry(1, Arrays.asList(1, 2, 3)),
        Map.entry(2, Arrays.asList(4, 5, 6)),
        Map.entry(3, Arrays.asList(7, 8, 9))
    );

    List<ReadEvent> events =new ArrayList<>();
    events.add(ReadEvent.startBody());
    events.add(ReadEvent.number(1));
    events.add(ReadEvent.slot());

    events.addAll(ArrayListRecognizerTest.listReadEvents(0,3));

    events.add(ReadEvent.number(2));
    events.add(ReadEvent.slot());

    events.addAll(ArrayListRecognizerTest.listReadEvents(3,3));

    events.add(ReadEvent.number(3));
    events.add(ReadEvent.slot());

    events.addAll(ArrayListRecognizerTest.listReadEvents(6,3));

    events.add(ReadEvent.endRecord());

//    Recognizer<HashMap<Integer, ArrayList<Integer>>> recognizer = new HashMapRecognizer<>(Integer.class, ArrayList.class, false);

//    HashMap<Integer, ArrayList<Integer>> actual = RecognizerTestUtil.runTest(recognizer, events);
  }

  @Test
  @SuppressWarnings("unchecked")
  void t() {
    Class<List<Object>> listClass = (Class<List<Object>>) (Class<?>) List.class;

  }

}
