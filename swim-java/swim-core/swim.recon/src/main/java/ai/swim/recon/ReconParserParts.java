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
import ai.swim.recon.models.identifier.Identifier;
import ai.swim.recon.models.identifier.StringIdentifier;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.*;

import java.util.Optional;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.DataParser.blob;
import static ai.swim.codec.parsers.OptParser.opt;
import static ai.swim.codec.parsers.ParserExt.alt;
import static ai.swim.codec.parsers.ParserExt.peek;
import static ai.swim.codec.parsers.StringParsersExt.*;
import static ai.swim.codec.parsers.number.NumberParser.numericLiteral;
import static ai.swim.codec.parsers.string.StringParser.stringLiteral;
import static ai.swim.recon.IdentifierParser.identifier;

public abstract class ReconParserParts {

  private static ReadEvent mapIdentifier(Identifier identifier) {
    if (identifier.isBoolean()) {
      return ReadEvent.bool(((BooleanIdentifier) identifier).getValue());
    } else {
      return ReadEvent.text(((StringIdentifier) identifier).getValue());
    }
  }

  public static Parser<ParserTransition> parseInit() {
    return alt(stringLiteral().map(t -> ReadEvent.text(t).transition()),
        identifier().map(s -> mapIdentifier(s).transition()),
        numericLiteral().map(n -> ReadEvent.number(n).transition()), blob().map(b -> ReadEvent.blob(b).transition()),
        secondaryAttr(), eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(),
            new ChangeState(ParseEvents.ParseState.RecordBodyStartOrNl))));
  }

  public static Parser<String> attr() {
    return preceded(eqChar('@'), attrName());
  }

  private static Parser<String> attrName() {
    return alt(
        stringLiteral(),
        identifier().tryMap(i -> {
          if (i.isText()) {
            return ((StringIdentifier) i).getValue();
          } else {
            throw new IllegalStateException("Expected a string identifier");
          }
        })
    );
  }

  public static Parser<ParserTransition> secondaryAttr() {
    return attr().map(ReadEvent::text).andThen(ReconParserParts::secondaryAttrCont);
  }

  private static Parser<ParserTransition> secondaryAttrCont(ReadEvent event) {
    return Parser.lambda(input -> {
      if (input.isDone()) {
        return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()),
            ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
      } else if (input.isError()) {
        return Parser.error(((InputError) input).getCause());
      } else if (input.isContinuation()) {
        Parser<Optional<Character>> parseResult = opt(eqChar('(')).feed(input);
        if (parseResult.isDone()) {
          if (parseResult.bind().isPresent()) {
            return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()),
                new PushAttr()));
          } else {
            return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()),
                ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
          }
        } else if (parseResult.isError()) {
          return ParserError.error(((ParserError<?>) parseResult).getCause());
        } else {
          return secondaryAttrCont(event);
        }
      } else {
        return secondaryAttrCont(event);
      }
    });
  }

  public static Parser<ParserTransition> primaryAttr() {
    return attr().map(ReadEvent::text).andThen(ReconParserParts::primaryAttrCont);
  }

  public static Parser<ParserTransition> primaryAttrCont(ReadEvent event) {
    return Parser.lambda(input -> {
      if (input.isDone()) {
        return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()), ReadEvent.endRecord(),
            new PushAttrNewRec(false)));
      } else if (input.isError()) {
        return Parser.error(((InputError) input).getCause());
      } else if (input.isContinuation()) {
        Parser<Optional<Character>> parseResult = opt(eqChar('(')).feed(input);
        if (parseResult.isDone()) {
          if (parseResult.bind().isPresent()) {
            return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()),
                new PushAttrNewRec(true)));
          } else {
            return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).value()), ReadEvent.endRecord(),
                new PushAttrNewRec(false)));
          }
        } else if (parseResult.isError()) {
          return ParserError.error(((ParserError<?>) parseResult).getCause());
        } else {
          return secondaryAttrCont(event);
        }
      } else {
        return secondaryAttrCont(event);
      }
    });
  }

  public static Parser<ParserTransition> parseAfterAttr() {
    return alt(
        alt(
            stringLiteral().map(ReadEvent::text),
            identifier().map(ReconParserParts::mapIdentifier),
            numericLiteral().map(ReadEvent::number),
            blob().map(ReadEvent::blob)
        ).map(event -> new ParserTransition(event, new PopAfterItem())),
        eqChar('{').map(c ->
            new ParserTransition(ReadEvent.startBody(), new ChangeState(ParseEvents.ParseState.RecordBodyStartOrNl))
        ),
        peek(alt(
            oneOf(',', ';', ')', '}').map(Object::toString),
            lineEnding()
        ).map(s -> new ParserTransition(ReadEvent.startBody(), ReadEvent.endRecord(), new PopAfterItem())))
    );
  }

  private static ParserTransition valueItem(ItemsKind itemsKind, ReadEvent readEvent) {
    return new ParserTransition(readEvent, new ChangeState(itemsKind.afterValue()));
  }

  public static Parser<Character> separator() {
    return oneOf(',', ';');
  }

  public static Parser<ParserTransition> parseNotAfterItem(ItemsKind itemsKind, boolean itemsRequired) {
    return alt(
        stringLiteral().map(s -> valueItem(itemsKind, ReadEvent.text(s))),
        identifier().map(i -> valueItem(itemsKind, mapIdentifier(i))),
        numericLiteral().map(n -> valueItem(itemsKind, ReadEvent.number(n))),
        blob().map(b -> valueItem(itemsKind, ReadEvent.blob(b))),
        separator().map(c -> new ParserTransition(ReadEvent.extant(), new ChangeState(itemsKind.afterSep()))),
        eqChar(':').map(c -> new ParserTransition(ReadEvent.extant(), ReadEvent.slot(), new ChangeState(itemsKind.startSlot()))),
        eqChar(itemsKind.endDelim()).map(c -> {
          ReadEvent event = itemsKind.endEvent();
          ParseEvents events;

          if (itemsRequired) {
            events = ParseEvents.twoEvents(ReadEvent.extant(), event);
          } else {
            events = ParseEvents.singleEvent(event);
          }

          return new ParserTransition(events, itemsKind.endStateChange());
        }),
        primaryAttr(),
        eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(), new PushBody()))
    );
  }

  public static Parser<Optional<ParseEvents.ParseState>> parseAfterValue(ItemsKind itemsKind) {
    return alt(
        eqChar(':').map(i -> Optional.of(itemsKind.startSlot())),
        parseAfterSlot(itemsKind)
    );
  }

  public static Parser<Optional<ParseEvents.ParseState>> parseAfterSlot(ItemsKind itemsKind) {
    return alt(
        lineEnding().map(i -> Optional.of(itemsKind.startOrNl())),
        separator().map(i -> Optional.of(itemsKind.afterSep())),
        eqChar(itemsKind.endDelim()).map(i -> Optional.empty())
    );
  }

  private static ParserTransition slotItem(ItemsKind itemsKind, ReadEvent readEvent) {
    return new ParserTransition(readEvent, new ChangeState(itemsKind.afterSlot()));
  }

  public static Parser<ParserTransition> parseSlotValue(ItemsKind itemsKind) {
    return alt(
        stringLiteral().map(t -> slotItem(itemsKind, ReadEvent.text(t))),
        identifier().map(i -> slotItem(itemsKind, mapIdentifier(i))),
        numericLiteral().map(n -> slotItem(itemsKind, ReadEvent.number(n))),
        blob().map(b -> slotItem(itemsKind, ReadEvent.blob(b))),
        lineEnding().map(c -> new ParserTransition(ReadEvent.extant(), new ChangeState(itemsKind.startOrNl()))),
        separator().map(s -> new ParserTransition(ReadEvent.extant(), new ChangeState(itemsKind.afterSep()))),
        eqChar(itemsKind.endDelim()).map(c -> new ParserTransition(ReadEvent.extant(), itemsKind.endEvent(), itemsKind.endStateChange())),
        primaryAttr(),
        eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(), new PushBody()))
    );
  }
}

