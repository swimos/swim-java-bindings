// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.recognizer.value;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.value.Item;
import ai.swim.structure.value.Record;
import ai.swim.structure.value.Value;
import ai.swim.structure.value.ValueItem;

import java.util.List;

class AttrBodyValueRecognizer extends ValueRecognizer {
  private ValueRecognizer valueRecognizer = new ValueRecognizer();

  public AttrBodyValueRecognizer() {
    stack.push(new ValueRecognizer.IncrementalValueBuilder(null, true));
  }

  @Override
  public Recognizer<Value> feedEvent(ReadEvent event) {
    if (stack.size() == 1) {
      if (event.isEndAttribute()) {
        ValueRecognizer.IncrementalValueBuilder builder = stack.pollFirst();
        Record.Builder record = builder.record;
        int itemCount = record.itemCount();

        if (itemCount == 0) {
          return Recognizer.done(Value.extant(), this);
        } else if (itemCount == 1) {
          Item item = record.popItem();
          if (item.isSlot()) {
            return Recognizer.done(Value.ofItems(List.of(item)), this);
          } else {
            return Recognizer.done(((ValueItem) item).getValue(), this);
          }
        } else {
          return Recognizer.done(record.build(), this);
        }
      } else if (event.isEndRecord()) {
        return Recognizer.error(new RecognizerException("Recognizer underflow"));
      } else {
        return onEvent(event);
      }
    }
    return onEvent(event);
  }

  private Recognizer<Value> onEvent(ReadEvent event) {
    Recognizer<Value> recognizer = super.feedEvent(event);
    if (recognizer.isDone()) {
      return Recognizer.error(new RecognizerException("Inconsistent state"));
    } else if (recognizer.isError()) {
      return recognizer;
    } else {
      return this;
    }
  }

  @Override
  public Recognizer<Value> reset() {
    return new AttrBodyValueRecognizer();
  }
}
