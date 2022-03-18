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

public class ParserExt {

  /***
   * Runs n parsers until one produces a value or every parser fails.
   **/
  @SafeVarargs
  public static <O> Parser<O> alt(Parser<O>... parsers) {
    return Parser.lambda(input -> {
      Parser<O> error = null;
      int errorCount = 0;

      for (int i = 0; i < parsers.length; i++) {
        Parser<O> p = parsers[i];

        if (p.isError()) {
          continue;
        }

        Parser<O> parseResult = p.feed(input.clone());

        if (parseResult.isError()) {
          errorCount += 1;
          error = parseResult;
        } else if (parseResult.isDone()) {
          return parseResult;
        }

        parsers[i] = parseResult;
      }

      if (errorCount == parsers.length) {
        return error;
      }

      return alt(parsers);
    });
  }

}
