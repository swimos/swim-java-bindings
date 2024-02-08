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

package ai.swim.structure.recognizer.value;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.value.Item;
import ai.swim.structure.value.Value;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ValueRecognizerTest {
  @Test
  void simpleValue() {
    Recognizer<Value> recognizer = new ValueRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("attr"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord()
                                    );

    for (int i = 0; i < events.size(); i++) {
      recognizer = recognizer.feedEvent(events.get(i));
      if (recognizer.isDone()) {
        recognizer = recognizer.feedEvent(events.get(events.size() - 1));
        assertTrue(recognizer.isDone());

        Value expected = Value.of(
            List.of(Value.ofAttr("attr")),
            List.of(
                Item.of(Value.of("a"), Value.of(1)),
                Item.of(Value.of("b"), Value.of(2))
                   )
                                 );

        assertEquals(expected, recognizer.bind());
      } else if (recognizer.isError()) {
        fail(recognizer.trap());
      }
    }
  }

  @Test
  void complexRecord() {
    Recognizer<Value> recognizer = new ValueRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("attr"),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.startBody(),
        ReadEvent.number(1.1f),
        ReadEvent.endRecord(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("text"),
        ReadEvent.startAttribute("nestedAttr"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.number(3),
        ReadEvent.number(5),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
                                    );

    for (int i = 0; i < events.size(); i++) {
      recognizer = recognizer.feedEvent(events.get(i));
      if (recognizer.isDone()) {
        recognizer = recognizer.feedEvent(events.get(events.size() - 1));
        assertTrue(recognizer.isDone());

        Value expected = Value.of(
            List.of(Value.ofAttr("attr", Value.ofItems(
                List.of(
                    Item.of(Value.of(1)),
                    Item.of(Value.of(2)),
                    Item.of(Value.ofItems(List.of(Item.of(Value.of(1.1f)))))
                       )
                                                      ))),
            List.of(
                Item.of(Value.of("text")),
                Item.of(Value.of(
                    List.of(Value.ofAttr("nestedAttr")),
                    List.of(
                        Item.of(Value.of(3)),
                        Item.of(Value.of(5))
                           )
                                ))
                   )
                                 );

        assertEquals(expected, recognizer.bind());
      } else if (recognizer.isError()) {
        fail(recognizer.trap());
      }
    }
  }
}