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

public class RecognizerRequired<T> extends Recognizer<T> {
  private Recognizer<T> delegate;

  public RecognizerRequired(Recognizer<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    this.delegate = this.delegate.feedEvent(event);
    if (this.delegate.isDone()) {
      T output = this.delegate.bind();
      if (output == null) {
        return Recognizer.error(new NullPointerException());
      } else {
        return Recognizer.done(output, this);
      }
    } else if (this.delegate.isError()) {
      return Recognizer.error(this.delegate.trap());
    }

    return this;
  }

  @Override
  public Recognizer<T> reset() {
    return new RecognizerRequired<>(this.delegate.reset());
  }

}
