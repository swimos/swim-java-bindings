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
import ai.swim.recon.event.ReadStartAttribute;
import ai.swim.structure.recognizer.Recognizer;

public class EnumRecognizer<T extends Enum<T>> extends Recognizer<T> {

  private final Class<T> clazz;
  private State state;
  private T target;

  public EnumRecognizer(Class<T> clazz) {
    this.clazz = clazz;
    this.state = State.None;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (this.state == State.None && event.isStartAttribute()) {
      String variant = ((ReadStartAttribute) event).value();

      for (T constant : this.clazz.getEnumConstants()) {
        if (constant.name().equalsIgnoreCase(variant)) {
          this.target = constant;
          this.state = State.EndAttribute;
          return this;
        }
      }

      return Recognizer.error(new RuntimeException(String.format(
          "'%s' is not a variant of %s",
          variant,
          clazz.getCanonicalName())));
    } else if (this.state == State.EndAttribute && event.isEndAttribute()) {
      this.state = State.StartBody;
      return this;
    } else if (this.state == State.StartBody && event.isStartBody()) {
      this.state = State.EndRecord;
      return this;
    } else if (this.state == State.EndRecord && event.isEndRecord()) {
      return Recognizer.done(this.target, this);
    } else {
      return Recognizer.error(new RuntimeException("Unexpected event type: " + event));
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new EnumRecognizer<>(this.clazz);
  }


  enum State {
    None,
    EndAttribute,
    StartBody,
    EndRecord
  }

}
