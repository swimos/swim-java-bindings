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

import ai.swim.recon.event.ReadBlobValue;
import ai.swim.recon.event.ReadBooleanValue;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.event.number.ReadBigDecimalValue;
import ai.swim.recon.event.number.ReadBigIntValue;
import ai.swim.recon.event.number.ReadDoubleValue;
import ai.swim.recon.event.number.ReadFloatValue;
import ai.swim.recon.event.number.ReadIntValue;
import ai.swim.recon.event.number.ReadLongValue;
import ai.swim.structure.recognizer.Recognizer;
import java.util.Collections;
import java.util.List;

public class UntypedRecognizer<T> extends Recognizer<T> {
  private Recognizer<Object> nested;
  private State state;
  private Object keyOrValue;

  public UntypedRecognizer() {
    this.state = State.Init;
    this.nested = null;
  }

  static <P> Recognizer<P> done(Recognizer<P> recognizer, Object value) {
    @SuppressWarnings("unchecked") P typed = (P) value;
    return Recognizer.done(typed, recognizer);
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (this.state) {
      case Init:
        if (event.isExtant()) {
          return Recognizer.done(null, this);
        } else if (event.isBlob()) {
          return done(this, ((ReadBlobValue) event).getValue());
        } else if (event.isBoolean()) {
          return done(this, ((ReadBooleanValue) event).getValue());
        } else if (event.isReadInt()) {
          return done(this, ((ReadIntValue) event).getValue());
        } else if (event.isReadLong()) {
          return done(this, ((ReadLongValue) event).getValue());
        } else if (event.isReadFloat()) {
          return done(this, ((ReadFloatValue) event).getValue());
        } else if (event.isReadDouble()) {
          return done(this, ((ReadDoubleValue) event).getValue());
        } else if (event.isReadBigInt()) {
          return done(this, ((ReadBigIntValue) event).getValue());
        } else if (event.isReadBigDecimal()) {
          return done(this, ((ReadBigDecimalValue) event).getValue());
        } else if (event.isText()) {
          return done(this, ((ReadTextValue) event).getValue());
        } else if (event.isStartBody()) {
          this.state = State.Between;
          return this;
        } else {
          return Recognizer.error(new RuntimeException("Unexpected read event: " + event));
        }
      case Between:
        if (nested == null) {
          if (event.isEndRecord()) {
            return done(this, Collections.emptyList());
          }

          this.nested = new UntypedRecognizer<>();
        }

        this.nested = this.nested.feedEvent(event);

        if (this.nested.isDone()) {
          this.keyOrValue = this.nested.bind();
          this.state = State.KeyOrValue;
          this.nested = null;
          return this;
        } else if (this.nested.isCont()) {
          return this;
        } else if (this.nested.isError()) {
          return Recognizer.error(this.nested.trap());
        } else {
          throw new AssertionError();
        }
      case KeyOrValue:
        if (event.isSlot()) {
          return new UntypedMapRecognizer<>(this.keyOrValue);
        } else if (event.isEndRecord()) {
          return done(this, List.of(this.keyOrValue));
        } else {
          return new UntypedListRecognizer<T>(this.keyOrValue).feedEvent(event);
        }
      default:
        throw new AssertionError(this.state);
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new UntypedRecognizer<>();
  }

  private enum State {
    Init,
    Between,
    KeyOrValue,
  }

}
