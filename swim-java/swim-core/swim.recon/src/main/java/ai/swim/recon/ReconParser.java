package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.events.EndParseEvent;
import ai.swim.recon.models.events.NoParseEvent;
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.*;
import ai.swim.recon.result.ParseResult;

import java.util.ArrayDeque;
import java.util.Deque;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.string.StringExt.multispace0;
import static ai.swim.codec.parsers.string.StringExt.space0;
import static ai.swim.recon.ReconParserParts.*;

public final class ReconParser {

  private final Input input;
  private final Deque<ParseEvents.ParseState> state;
  private Parser<ParserTransition> current;

  public ReconParser(Input input) {
    this.input = input;
    this.state = new ArrayDeque<>(10);
  }

  public boolean isError() {
    throw new AssertionError();
  }

  public boolean isCont() {
    throw new AssertionError();
  }

  public boolean isDone() {
    throw new AssertionError();
  }

  public ReconParser feed(Input input) {
    return new ReconParser(input);
  }

  public ParseResult<ParseEvents> next() {
    if (this.state.peekLast() != null) {
      return this.nextEvent();
    } else {
      return ParseResult.ok(new EndParseEvent());
    }
  }

  private ParseResult<ParseEvents> parseEvent(Parser<ParserTransition> parser, boolean clearIfNone) {
    if (this.current == null) {
      this.current = parser;
      return parseEvent(null, clearIfNone);
    }

    if (this.current.isCont()) {
      return ParseResult.continuation();
    } else if (this.current.isError()) {
      return ParseResult.error(((ParserError<ParserTransition>) this.current).getCause());
    }

    ParserTransition output = this.current.bind();
    this.transition(output.getChange(), clearIfNone);
    this.current = null;
    return ParseResult.ok(output.getEvents());
  }

  private ParseResult<ParseEvents> nextEvent() {
    switch (this.state.getLast()) {
      case Init:
        return parseEvent(preceded(multispace0(), parseInit()), true);
      case AfterAttr:
        return parseEvent(parseAfterAttr(), false);
      case RecordBodyStartOrNl:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.record(), false)), false);
      case AttrBodyStartOrNl:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.attr(), false)), false);
      case RecordBodyAfterSep:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.record(), true)), false);
      case AttrBodyAfterSep:
        return parseEvent(preceded(multispace0(), parseNotAfterItem(ItemsKind.attr(), true)), false);
      case RecordBodyAfterValue:
        return parseEvent(preceded(space0(), parseAfterValue(ItemsKind.record())).map(s -> {
          if (s.isPresent()) {
            ParseEvents.ParseState parseState = s.get();
            this.state.removeLast();
            this.state.addLast(parseState);

            if (parseState == ParseEvents.ParseState.AttrBodySlot) {
              return new ParserTransition(ReadEvent.slot(), null);
            } else {
              return new ParserTransition(new NoParseEvent(), null);
            }
          } else {
            this.transition(new PopAfterItem(), false);
            return new ParserTransition(ReadEvent.endRecord(), null);
          }
        }), false);
      case RecordBodyAfterSlot:
        return parseEvent(preceded(space0(), parseAfterSlot(ItemsKind.record())).map(s -> {
          if (s.isPresent()) {
            this.state.removeLast();
            this.state.addLast(s.get());
            return new ParserTransition(new NoParseEvent(), null);
          } else {
            this.transition(new PopAfterItem(), false);
            return new ParserTransition(ReadEvent.endRecord(), null);
          }
        }), false);
      case AttrBodyAfterSlot:
        return parseEvent(preceded(space0(), parseAfterSlot(ItemsKind.attr())).map(s -> {
          if (s.isPresent()) {
            this.state.removeLast();
            this.state.addLast(s.get());
            return new ParserTransition(new NoParseEvent(), null);
          } else {
            this.transition(new PopAfterAttr(), false);
            return new ParserTransition(ReadEvent.endAttribute(), null);
          }
        }), false);
      case AttrBodyAfterValue:
        return parseEvent(preceded(space0(), parseAfterValue(ItemsKind.record())).map(s -> {
          if (s.isPresent()) {
            ParseEvents.ParseState parseState = s.get();
            this.state.removeLast();
            this.state.addLast(parseState);

            if (parseState == ParseEvents.ParseState.AttrBodySlot) {
              return new ParserTransition(ReadEvent.slot(), null);
            } else {
              return new ParserTransition(new NoParseEvent(), null);
            }
          } else {
            this.transition(new PopAfterAttr(), false);
            return new ParserTransition(ReadEvent.endAttribute(), null);
          }
        }), false);
      case RecordBodySlot:
        return parseEvent(preceded(space0(), parseSlotValue(ItemsKind.record())), false);
      default:
        throw new AssertionError();
      case AttrBodySlot:
        return parseEvent(preceded(space0(), parseSlotValue(ItemsKind.attr())), false);
    }
  }

  private void transition(StateChange stateChange, boolean clearIfNone) {
    if (stateChange.isNone()) {
      if (clearIfNone) {
        this.state.clear();
      }
    } else if (stateChange.isPopAfterAttr()) {
      if (this.state.peekLast() != null) {
        this.state.removeLast();
        this.state.addLast(ParseEvents.ParseState.AfterAttr);
      }
    } else if (stateChange.isPopAfterItem()) {
      ParseEvents.ParseState parseState = this.state.peekLast();
      if (parseState != null) {
        this.state.removeLast();
        switch (parseState) {
          case Init:
            this.state.addLast(ParseEvents.ParseState.AfterAttr);
            break;
          case AttrBodyStartOrNl:
          case AttrBodyAfterSep:
            this.state.addLast(ParseEvents.ParseState.AttrBodyAfterValue);
            break;
          case AttrBodySlot:
            this.state.addLast(ParseEvents.ParseState.AttrBodyAfterSlot);
            break;
          case RecordBodyStartOrNl:
          case RecordBodyAfterSep:
            this.state.addLast(ParseEvents.ParseState.RecordBodyAfterValue);
            break;
          case RecordBodySlot:
            this.state.addLast(ParseEvents.ParseState.RecordBodyAfterSlot);
            break;
          default:
            throw new IllegalStateException("Invalid state transition from: " + parseState + ", to: " + stateChange);
        }
      }
    } else if (stateChange.isChangeState()) {
      if (this.state.peekLast() != null) {
        this.state.removeLast();
        this.state.addLast(((ChangeState) stateChange).getState());
      }
    } else if (stateChange.isPushAttr()) {
      this.state.addLast(ParseEvents.ParseState.AttrBodyStartOrNl);
    } else if (stateChange.isPushAttrNewRec()) {
      PushAttrNewRec pushAttr = (PushAttrNewRec) stateChange;
      if (pushAttr.hasBody()) {
        this.state.addLast(ParseEvents.ParseState.Init);
        this.state.addLast(ParseEvents.ParseState.AttrBodyStartOrNl);
      } else {
        this.state.addLast(ParseEvents.ParseState.AfterAttr);
      }
    } else if (stateChange.isPushAttrNewRec()) {
      this.state.addLast(ParseEvents.ParseState.AttrBodyStartOrNl);
    } else if (stateChange.isPushBody()) {
      this.state.addLast(ParseEvents.ParseState.RecordBodyStartOrNl);
    } else {
      throw new AssertionError();
    }
  }

}
