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

package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.proxy.RecognizerProxy;

public class FieldRecognizingBuilder<I> implements RecognizingBuilder<I> {

  public Recognizer<I> recognizer;
  public I value;

  public FieldRecognizingBuilder(Class<I> clazz) {
    this.recognizer = RecognizerProxy.getProxy().lookup(clazz);
  }

  public FieldRecognizingBuilder(Recognizer<I> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public boolean feed(ReadEvent event) {
    if (this.value != null) {
      throw new RecognizerException("Duplicate value");
    }

    Recognizer<I> feedResult = this.recognizer.feedEvent(event);
    if (feedResult.isDone()) {
      value = feedResult.bind();
      return true;
    } else if (feedResult.isError()) {
      throw feedResult.trap();
    } else {
      this.recognizer = feedResult;
      return false;
    }
  }

  @Override
  public I bind() {
    return this.value;
  }

  @Override
  public I bindOr(I defaultValue) {
    if (this.value == null) {
      return defaultValue;
    } else {
      return this.value;
    }
  }

  @Override
  public RecognizingBuilder<I> reset() {
    return new FieldRecognizingBuilder<>(this.recognizer.reset());
  }

}
