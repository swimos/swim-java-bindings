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

package ai.swim.codec.parsers.text;

class UnicodeParser {
  int unicodeIdx;
  int code;
  boolean done;

  boolean parseUnicodePoint(StringBuilder builder, int c) {
    if (StringParser.isDigit(c)) {
      code = 16 * code + StringParser.decodeDigit(c);
      if (unicodeIdx <= 3) {
        unicodeIdx += 1;
      } else {
        builder.appendCodePoint(code);
        code = 0;
        done = true;
      }
      return true;
    } else {
      return false;
    }
  }

  boolean isDone() {
    return done;
  }

}
