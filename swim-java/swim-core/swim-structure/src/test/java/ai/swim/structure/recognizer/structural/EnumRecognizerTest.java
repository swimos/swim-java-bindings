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

package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static ai.swim.structure.RecognizerTestUtil.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumRecognizerTest {

  @Test
  void readSimpleEnum() {
    Recognizer<Level> recognizer = new EnumRecognizer<>(Level.class);
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("warn"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
                                    );

    Level level = runTest(recognizer, events);
    assertEquals(level, Level.Warn);
  }

  enum Level {
    Info(0),
    Warn(1),
    Error(2);

    private final int idx;

    Level(int idx) {
      this.idx = idx;
    }
  }

}
