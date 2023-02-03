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
import ai.swim.codec.input.Input;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.recon.models.ParseState;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.identifier.BooleanIdentifier;
import ai.swim.recon.models.identifier.Identifier;
import ai.swim.recon.models.identifier.StringIdentifier;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.ModifyState;
import ai.swim.recon.models.state.PushAttrNewRec;
import ai.swim.recon.models.state.StateChange;

import java.util.List;
import java.util.Optional;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.DataParser.blob;
import static ai.swim.codec.parsers.OptParser.opt;
import static ai.swim.codec.parsers.combinators.Alt.alt;
import static ai.swim.codec.parsers.combinators.Peek.peek;
import static ai.swim.codec.parsers.number.NumberParser.numericLiteral;
import static ai.swim.codec.parsers.text.EqChar.eqChar;
import static ai.swim.codec.parsers.text.LineEnding.lineEnding;
import static ai.swim.codec.parsers.text.OneOf.oneOf;
import static ai.swim.codec.parsers.text.StringParser.stringLiteral;
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
    return alt(
        stringLiteral().map(t -> ReadEvent.text(t).transition()),
        identifier().map(s -> mapIdentifier(s).transition()),
        numericLiteral().map(n -> ReadEvent.number(n).transition()),
        blob().map(b -> ReadEvent.blob(b).transition()),
        secondaryAttr(),
        eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(), new ModifyState(ParseState.RecordBodyStartOrNl)))
    );
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
    return new Parser<>() {
      @Override
      public Parser<ParserTransition> feed(Input input) {
        if (input.isDone()) {
          return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).getValue()),
              ReadEvent.endAttribute(), new ModifyState(ParseState.AfterAttr)));
        } else if (input.isContinuation()) {
          Parser<Optional<Character>> parseResult = opt(eqChar('(')).feed(input);
          if (parseResult.isDone()) {
            if (parseResult.bind().isPresent()) {
              return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).getValue()),
                  StateChange.pushAttr()));
            } else {
              return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).getValue()),
                  ReadEvent.endAttribute(), new ModifyState(ParseState.AfterAttr)));
            }
          } else if (parseResult.isError()) {
            return ParserError.error(input, ((ParserError<?>) parseResult).cause());
          } else {
            return this;
          }
        } else {
          return this;
        }
      }
    };
  }

  public static Parser<ParserTransition> primaryAttr() {
    return attr().map(ReadEvent::text).andThen(ReconParserParts::primaryAttrCont);
  }

  public static Parser<ParserTransition> primaryAttrCont(ReadEvent event) {
    return new Parser<>() {
      @Override
      public Parser<ParserTransition> feed(Input input) {
        if (input.isDone()) {
          return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).getValue()), ReadEvent.endRecord(),
              new PushAttrNewRec(false)));
        } else if (input.isContinuation()) {
          Parser<Optional<Character>> parseResult = opt(eqChar('(')).feed(input);
          if (parseResult.isDone()) {
            if (parseResult.bind().isPresent()) {
              return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).getValue()),
                  new PushAttrNewRec(true)));
            } else {
              return Parser.done(new ParserTransition(ReadEvent.startAttribute(((ReadTextValue) event).getValue()), ReadEvent.endAttribute(),
                  new PushAttrNewRec(false)));
            }
          } else if (parseResult.isError()) {
            return ParserError.error(input, ((ParserError<?>) parseResult).cause());
          } else {
            return this;
          }
        } else {
          return this;
        }
      }
    };
  }

  public static Parser<ParserTransition> parseAfterAttr() {
    return alt(
        secondaryAttr(),
        alt(
            stringLiteral().map(ReadEvent::text),
            identifier().map(ReconParserParts::mapIdentifier),
            numericLiteral().map(ReadEvent::number),
            blob().map(ReadEvent::blob)
        ).map(event -> new ParserTransition(List.of(ReadEvent.startBody(), event, ReadEvent.endRecord()), StateChange.popAfterItem())),
        eqChar('{').map(c ->
            new ParserTransition(ReadEvent.startBody(), new ModifyState(ParseState.RecordBodyStartOrNl))
        ),
        peek(alt(
            oneOf(',', ';', ')', '}').map(Object::toString),
            lineEnding()
        ).map(s -> new ParserTransition(ReadEvent.startBody(), ReadEvent.endRecord(), StateChange.popAfterItem())))
    );
  }

  private static ParserTransition valueItem(ItemsKind itemsKind, ReadEvent readEvent) {
    return new ParserTransition(readEvent, new ModifyState(itemsKind.afterValue()));
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
        separator().map(c -> new ParserTransition(ReadEvent.extant(), new ModifyState(itemsKind.afterSep()))),
        eqChar(':').map(c -> new ParserTransition(ReadEvent.extant(), ReadEvent.slot(), new ModifyState(itemsKind.startSlot()))),
        eqChar(itemsKind.endDelim()).map(c -> {
          ReadEvent event = itemsKind.endEvent();
          List<ReadEvent> events;

          if (itemsRequired) {
            events = List.of(ReadEvent.extant(), event);
          } else {
            events = List.of(event);
          }

          return new ParserTransition(events, itemsKind.endStateChange());
        }),
        primaryAttr(),
        eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(), StateChange.pushBody()))
    );
  }

  public static Parser<Optional<ParseState>> parseAfterValue(ItemsKind itemsKind) {
    return alt(
        eqChar(':').map(i -> Optional.of(itemsKind.startSlot())),
        parseAfterSlot(itemsKind)
    );
  }

  public static Parser<Optional<ParseState>> parseAfterSlot(ItemsKind itemsKind) {
    return alt(
        lineEnding().map(i -> Optional.of(itemsKind.startOrNl())),
        separator().map(i -> Optional.of(itemsKind.afterSep())),
        eqChar(itemsKind.endDelim()).map(i -> Optional.empty())
    );
  }

  private static ParserTransition slotItem(ItemsKind itemsKind, ReadEvent readEvent) {
    return new ParserTransition(readEvent, new ModifyState(itemsKind.afterSlot()));
  }

  public static Parser<ParserTransition> parseSlotValue(ItemsKind itemsKind) {
    return alt(
        stringLiteral().map(t -> slotItem(itemsKind, ReadEvent.text(t))),
        identifier().map(i -> slotItem(itemsKind, mapIdentifier(i))),
        numericLiteral().map(n -> slotItem(itemsKind, ReadEvent.number(n))),
        blob().map(b -> slotItem(itemsKind, ReadEvent.blob(b))),
        lineEnding().map(c -> new ParserTransition(ReadEvent.extant(), new ModifyState(itemsKind.startOrNl()))),
        separator().map(s -> new ParserTransition(ReadEvent.extant(), new ModifyState(itemsKind.afterSep()))),
        eqChar(itemsKind.endDelim()).map(c -> new ParserTransition(ReadEvent.extant(), itemsKind.endEvent(), itemsKind.endStateChange())),
        primaryAttr(),
        eqChar('{').map(c -> new ParserTransition(ReadEvent.startBody(), StateChange.pushBody()))
    );
  }
}

