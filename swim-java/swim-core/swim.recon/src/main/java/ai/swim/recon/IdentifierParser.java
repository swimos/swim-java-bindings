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

package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.recon.models.identifier.Identifier;
import static ai.swim.recon.ReconUtils.isIdentChar;
import static ai.swim.recon.ReconUtils.isIdentStartChar;

public class IdentifierParser extends Parser<Identifier> {

  private final StringBuilder data;
  private State state;

  IdentifierParser() {
    this.data = new StringBuilder();
    this.state = State.Head;
  }

  public static IdentifierParser identifier() {
    return new IdentifierParser();
  }

  @Override
  public Parser<Identifier> feed(Input input) {
    int c;
    if (state == State.Head) {
      if (input.isContinuation()) {
        c = input.head();
        if (isIdentStartChar(c)) {
          input = input.step();
          data.appendCodePoint(c);
          state = State.Body;
        } else {
          return error("Expected an identifier");
        }
      } else if (input.isDone()) {
        return error("Expected an identifier");
      }
    }
    if (state == State.Body) {
      while (input.isContinuation()) {
        c = input.head();
        if (isIdentChar(c)) {
          input = input.step();
          data.appendCodePoint(c);
        } else {
          break;
        }
      }
      if (!input.isEmpty()) {
        String output = this.data.toString();
        if (output.equals("true")) {
          return Parser.done(Identifier.bool(true));
        } else if (output.equals("false")) {
          return Parser.done(Identifier.bool(false));
        } else if (output.equalsIgnoreCase("nan")) {
          return Parser.done(Identifier.decimal(Float.NaN));
        } else if (output.equalsIgnoreCase("-inf") || output.equalsIgnoreCase("-infinity")) {
          return Parser.done(Identifier.decimal(Float.NEGATIVE_INFINITY));
        } else if (output.equalsIgnoreCase("inf") || output.equalsIgnoreCase("infinity")) {
          return Parser.done(Identifier.decimal(Float.POSITIVE_INFINITY));
        } else {
          return Parser.done(Identifier.string(output));
        }
      }
    }

    if (input.isError()) {
      return error("Expected an identifier");
    }

    return this;
  }

  enum State {
    Head,
    Body
  }
}
