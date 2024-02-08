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

package ai.swim.structure.recognizer.untyped;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import java.util.ArrayList;
import java.util.List;

class UntypedListRecognizer<T> extends Recognizer<T> {
  private final List<Object> list;
  private Recognizer<Object> nested;

  UntypedListRecognizer(Object first) {
    this.list = new ArrayList<>(List.of(first));
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (event.isEndRecord() && this.nested == null) {
      return UntypedRecognizer.done(this, list);
    }

    if (this.nested == null) {
      this.nested = new UntypedRecognizer<>();
    }

    this.nested = this.nested.feedEvent(event);

    if (this.nested.isDone()) {
      list.add(this.nested.bind());
      this.nested = null;
      return this;
    } else if (this.nested.isCont()) {
      return this;
    } else if (this.nested.isError()) {
      return Recognizer.error(this.nested.trap());
    } else {
      throw new AssertionError();
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new UntypedRecognizer<>();
  }

}
