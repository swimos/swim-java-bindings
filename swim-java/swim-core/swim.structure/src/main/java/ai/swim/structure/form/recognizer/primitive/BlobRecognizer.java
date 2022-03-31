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

package ai.swim.structure.form.recognizer.primitive;

import ai.swim.recon.event.ReadBlobValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;

public class BlobRecognizer extends Recognizer<byte[]> {

  @Override
  public Recognizer<byte[]> feedEvent(ReadEvent event) {
    if (event.isBlob()) {
      ReadBlobValue readBlobValue = (ReadBlobValue) event;
      return Recognizer.done(readBlobValue.value());
    } else {
      return Recognizer.error(new RuntimeException("Expected a byte[]"));
    }
  }

}
