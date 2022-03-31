// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.codec.input;

import ai.swim.codec.location.Location;

public class InputError extends Input {
  private final String cause;

  public InputError(String cause) {
    this.cause = cause;
  }

  @Override
  public boolean has(int n) {
    return false;
  }

  @Override
  public int head() {
    return 0;
  }

  @Override
  public Input step() {
    throw new IllegalStateException();
  }

  @Override
  public Location location() {
    return null;
  }

  @Override
  public boolean isDone() {
    return false;
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
  public Input isPartial(boolean isPartial) {
    return null;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public int[] bind() {
    return new int[0];
  }

  @Override
  public int len() {
    return 0;
  }

  @Override
  public int[] take(int n) {
    return new int[0];
  }

  @Override
  public Input clone() {
    throw new IllegalStateException();
  }

  @Override
  public Input extend(Input from) {
    throw new IllegalStateException();
  }

  public String cause() {
    return this.cause;
  }
}
