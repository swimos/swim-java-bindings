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

package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;

import java.util.Optional;

import static ai.swim.codec.parsers.OptParser.opt;
import static ai.swim.codec.parsers.text.EqChar.eqChar;

public class StringExt {

  public static Parser<Optional<Character>> space0() {
    return opt(eqChar(' '));
  }

}
