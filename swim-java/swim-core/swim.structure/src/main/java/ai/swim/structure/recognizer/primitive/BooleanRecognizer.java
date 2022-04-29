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

package ai.swim.structure.recognizer.primitive;

import ai.swim.recon.event.ReadBooleanValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;

public class BooleanRecognizer extends Recognizer<Boolean> {

  public static final BooleanRecognizer INSTANCE = new BooleanRecognizer();

  private BooleanRecognizer() {

  }

  @Override
  public Recognizer<Boolean> feedEvent(ReadEvent event) {
    if (event.isBoolean()) {
      ReadBooleanValue readBooleanValue = (ReadBooleanValue) event;
      return Recognizer.done(readBooleanValue.value());
    } else {
      return Recognizer.error(new RuntimeException("Expected a boolean"));
    }
  }

  @Override
  public Recognizer<Boolean> reset() {
    return INSTANCE;
  }

}
