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

package ai.swim.client.downlink.value;

import ai.swim.client.downlink.DownlinkException;
import ai.swim.client.lifecycle.OnEvent;
import ai.swim.client.lifecycle.OnSet;
import ai.swim.client.lifecycle.OnSynced;
import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.structure.Form;
import ai.swim.structure.FormParser;
import ai.swim.structure.recognizer.RecognizerException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class ValueDownlinkState<T> {
  private final Form<T> form;
  private T state;

  ValueDownlinkState(Form<T> form) {
    this.form = form;
  }

  Consumer<ByteBuffer> wrapOnEvent(OnEvent<T> onEvent) {
    if (onEvent != null) {
      return buffer -> {
        T value;
        try {
          value = parse(buffer);
        } catch (RuntimeException e) {
          throw new DownlinkException("Invalid frame body", e);
        }

        try {
          onEvent.onEvent(value);
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      };
    } else {
      return null;
    }
  }

  Consumer<ByteBuffer> wrapOnSynced(OnSynced<T> onSynced) {
    if (onSynced != null) {
      return buffer -> {
        T value;
        try {
          value = parse(buffer);
        } catch (RuntimeException e) {
          throw new DownlinkException("Invalid frame body", e);
        }

        try {
          this.state = value;
          onSynced.onSynced(value);
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      };
    } else {
      return null;
    }
  }

  Consumer<ByteBuffer> wrapOnSet(OnSet<T> onSet) {
    if (onSet != null) {
      return buffer -> {
        T value;
        try {
          value = parse(buffer);
        } catch (RuntimeException e) {
          throw new DownlinkException("Invalid frame body", e);
        }

        try {
          onSet.onSet(state, value);
          this.state = value;
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      };
    } else {
      return null;
    }
  }

  private T parse(ByteBuffer buffer) {
    Parser<T> parser = new FormParser<>(form.reset());
    parser = parser.feed(Input.byteBuffer(buffer));
    if (parser.isDone()) {
      return parser.bind();
    } else if (parser.isError()) {
      ParserError<T> error = (ParserError<T>) parser;
      throw new RecognizerException(String.format("%s at: %s", error.cause(), error.location()));
    } else {
      throw new RecognizerException("Unconsumed input");
    }
  }

}
