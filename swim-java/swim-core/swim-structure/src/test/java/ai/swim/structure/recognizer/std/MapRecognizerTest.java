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

package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static ai.swim.structure.RecognizerTestUtil.runTest;

class MapRecognizerTest {

  @Test
  void testHashMap() {
    Recognizer<Map<String, Integer>> recognizer = new MapRecognizer<>(
        ScalarRecognizer.STRING,
        ScalarRecognizer.INTEGER);
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.text("c"),
        ReadEvent.slot(),
        ReadEvent.number(3),
        ReadEvent.endRecord()
                                    );

    runTest(recognizer, events);
  }

}