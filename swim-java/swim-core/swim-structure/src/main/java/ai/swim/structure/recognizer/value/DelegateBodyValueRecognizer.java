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
import ai.swim.structure.value.Item;
import ai.swim.structure.value.Record;
import ai.swim.structure.value.Value;
import ai.swim.structure.value.ValueItem;

import java.util.List;

class DelegateBodyValueRecognizer extends ValueRecognizer {
  @Override
  public Recognizer<Value> feedEvent(ReadEvent event) {
    return super.feedEvent(event).map(value -> {
      if (value.isRecord()) {
        Record record = (Record) value;
        if (record.getAttrCount() == 0 && record.getItemCount() <= 1) {
          Item item = record.getItems()[0];
          if (item != null) {
            if (item.isSlot()) {
              return Value.ofItems(List.of(item));
            } else {
              return ((ValueItem) item).getValue();
            }
          } else {
            return Value.extant();
          }
        } else {
          return value;
        }
      } else {
        return value;
      }
    });
  }

  @Override
  public Recognizer<Value> reset() {
    return new DelegateBodyValueRecognizer();
  }
}
