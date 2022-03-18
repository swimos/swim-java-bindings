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
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.BooleanIdentifier;
import ai.swim.recon.models.StringIdentifier;
import static ai.swim.codec.DataParser.blob;
import static ai.swim.codec.ParserExt.alt;
import static ai.swim.codec.number.NumberParser.numericLiteral;
import static ai.swim.codec.string.StringParser.stringLiteral;
import static ai.swim.recon.IdentifierParser.identifier;

public abstract class ReconParser {

  public static Parser<ReadEvent> init() {
    return alt(
        stringLiteral().map(ReadEvent::text),
        identifier().map(s -> {
          if (s.isBoolean()) {
            return ReadEvent.bool(((BooleanIdentifier) s).getValue());
          } else {
            return ReadEvent.text(((StringIdentifier) s).getValue());
          }
        }),
        numericLiteral().map(ReadEvent::number)
//        blob().map(ReadEvent::blob)
    );
  }


}

