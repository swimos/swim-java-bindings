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

package ai.swim.codec;

import ai.swim.codec.input.Input;

public class ParserError<O> extends Parser<O> {
  private final String cause;

  public ParserError(String cause) {
    this.cause = cause;
  }

  @Override
  public Parser<O> feed(Input input) {
    throw new IllegalStateException();
  }

  public String getCause() {
    return cause;
  }

  @Override
  public boolean isError() {
    return true;
  }
}
