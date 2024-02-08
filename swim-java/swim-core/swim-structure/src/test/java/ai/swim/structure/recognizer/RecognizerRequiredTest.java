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

package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecognizerRequiredTest {
  @Test
  void testRequired() {
    Recognizer<Integer> integerRecognizer = ScalarRecognizer.INTEGER.required();
    integerRecognizer = integerRecognizer.feedEvent(ReadEvent.extant());

    assertTrue(integerRecognizer.isError());
  }
}