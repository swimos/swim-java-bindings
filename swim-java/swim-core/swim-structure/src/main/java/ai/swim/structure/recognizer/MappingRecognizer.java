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
import java.util.function.Function;

public class MappingRecognizer<I, O> extends Recognizer<O> {

  private final Function<I, O> mapFn;
  private Recognizer<I> delegate;

  public MappingRecognizer(Recognizer<I> delegate, Function<I, O> mapFn) {
    this.delegate = delegate;
    this.mapFn = mapFn;
  }

  @Override
  public Recognizer<O> feedEvent(ReadEvent event) {
    this.delegate = this.delegate.feedEvent(event);
    if (delegate.isDone()) {
      return Recognizer.done(this.mapFn.apply(this.delegate.bind()), this);
    } else if (this.delegate.isError()) {
      return Recognizer.error(this.delegate.trap());
    } else {
      return this;
    }
  }

  @Override
  public Recognizer<O> reset() {
    return new MappingRecognizer<>(this.delegate.reset(), this.mapFn);
  }
}
