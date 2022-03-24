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
import ai.swim.codec.ParserError;
import ai.swim.codec.input.InputError;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.identifier.BooleanIdentifier;
import ai.swim.recon.models.identifier.StringIdentifier;
import ai.swim.recon.models.state.ChangeState;
import ai.swim.recon.models.state.PushAttr;
import java.util.Optional;
import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.DataParser.blob;
import static ai.swim.codec.parsers.OptParser.opt;
import static ai.swim.codec.parsers.ParserExt.alt;
import static ai.swim.codec.parsers.number.NumberParser.numericLiteral;
import static ai.swim.codec.parsers.string.EqChar.eqChar;
import static ai.swim.codec.parsers.string.StringParser.stringLiteral;
import static ai.swim.recon.IdentifierParser.identifier;

public abstract class ReconParser {

  public static Parser<ParserTransition> parserInit() {
    return alt(
        stringLiteral().map(t -> ReadEvent.text(t).transition()),
        identifier().map(s -> {
          if (s.isBoolean()) {
            return ReadEvent.bool(((BooleanIdentifier) s).getValue()).transition();
          } else {
            return ReadEvent.text(((StringIdentifier) s).getValue()).transition();
          }
        }),
        numericLiteral().map(n -> ReadEvent.number(n).transition()),
        blob().map(b -> ReadEvent.blob(b).transition()),
        secondaryAttr(), eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(), new ChangeState(ParseEvents.ParseState.RecordBodyStartOrNl)))
    );
  }

  public static Parser<ParserTransition> secondaryAttr() {
    return preceded(eqChar('@'), alt(stringLiteral().map(ReadEvent::text), identifier().tryMap(r -> {
      if (r.isText()) {
        return ReadEvent.text(((StringIdentifier) r).getValue());
      } else {
        throw new IllegalStateException("Expected a text identifier");
      }
    }))).andThen(ReconParser::isBody);
  }

  private static Parser<ParserTransition> isBody(ReadEvent event) {
    return Parser.lambda(input -> {
      if (input.isDone()) {
        return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()), ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
      } else if (input.isError()) {
        return Parser.error(((InputError) input).getCause());
      } else if (input.isContinuation()) {
        Parser<Optional<Character>> parseResult = opt(eqChar('(')).feed(input);
        if (parseResult.isDone()) {
          if (parseResult.bind().isPresent()) {
            return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()), new PushAttr()));
          } else {
            return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()), ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
          }
        } else if (parseResult.isError()) {
          return ParserError.error(((ParserError<?>) parseResult).getCause());
        } else {
          return isBody(event);
        }
      } else {
        return isBody(event);
      }
    });
  }

}

