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

public abstract class SimpleRecognizer<T> extends Recognizer<T> {
  private final boolean allowExtant;
  private final String type;

  public SimpleRecognizer(boolean allowExtant, String type) {
    this.allowExtant = allowExtant;
    this.type = type;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    if (allowExtant && event.isExtant()) {
      return Recognizer.done(null, this);
    } else {
      T value = feed(event);
      if (value == null) {
        return Recognizer.error(new RecognizerException(String.format(String.format(
            "Found '%s', expected: '%s'",
            event,
            type))));
      } else {
        return Recognizer.done(value, this);
      }
    }
  }

  protected abstract T feed(ReadEvent event);

  @Override
  public Recognizer<T> reset() {
    return this;
  }
}
