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

package ai.swim.codec.input;

import ai.swim.codec.location.Location;

/**
 * An input in the done state that has no more symbols to produce.
 */
public class InputDone extends Input {

  private final Input delegate;

  public InputDone(Input delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean has(int n) {
    return false;
  }

  @Override
  public int head() {
    throw new IllegalStateException();
  }

  @Override
  public Input step() {
    throw new IllegalStateException();
  }

  @Override
  public Location location() {
    return this.delegate.location();
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isContinuation() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Input setPartial(boolean isPartial) {
    return this;
  }

  @Override
  public int[] bind() {
    return new int[] {};
  }

  @Override
  public void bind(int[] into) {

  }

  @Override
  public int len() {
    return this.delegate.len();
  }

  @Override
  public void take(int[] into) {

  }

  @Override
  public Input clone() {
    return new InputDone(this.delegate.clone());
  }

  @Override
  public Input extend(Input from) {
    return this.delegate.extend(from);
  }

}
