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

public class FirstOf<T> extends Recognizer<T> {
  private Recognizer<T> left;
  private Recognizer<T> right;
  private State state;

  public FirstOf(Recognizer<T> left, Recognizer<T> right) {
    this.left = left;
    this.right = right;
    this.state = State.Both;
  }

  @Override
  public Recognizer<T> feedEvent(ReadEvent event) {
    switch (state) {
      case Both:
        left = left.feedEvent(event);
        if (left.isDone()) {
          return Recognizer.done(left.bind(), this);
        } else if (left.isError()) {
          state = State.Right;
        }

        right = right.feedEvent(event);
        if (right.isDone()) {
          return Recognizer.done(right.bind(), this);
        } else if (right.isError()) {
          if (state == State.Both) {
            state = State.Left;
            return this;
          } else {
            return Recognizer.error(right.trap());
          }
        } else {
          return this;
        }
      case Left:
        left = left.feedEvent(event);
        return discriminate(left);
      case Right:
        right = right.feedEvent(event);
        return discriminate(right);
      default:
        throw new AssertionError();
    }
  }

  private Recognizer<T> discriminate(Recognizer<T> recognizer) {
    if (recognizer.isDone()) {
      return Recognizer.done(recognizer.bind(), this);
    } else if (recognizer.isError()) {
      return Recognizer.error(recognizer.trap());
    } else {
      return this;
    }
  }

  @Override
  public Recognizer<T> reset() {
    return new FirstOf<>(left.reset(), right.reset());
  }

  enum State {
    Both,
    Left,
    Right
  }
}
