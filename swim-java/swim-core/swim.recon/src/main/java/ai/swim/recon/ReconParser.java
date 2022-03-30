package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.events.Event;
import ai.swim.recon.models.events.EventOrEnd;
import ai.swim.recon.models.events.NoParseEvent;
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.*;
import ai.swim.recon.result.ParseResult;

import java.util.*;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.ParserExt.alt;
import static ai.swim.codec.parsers.string.StringExt.multispace0;
import static ai.swim.codec.parsers.string.StringExt.space0;
import static ai.swim.recon.ReconParserParts.*;

public final class ReconParser {

  private final Input input;
  private final Deque<ParseEvents.ParseState> state;
  private Parser<ParserTransition> current;
  private ParseEvents pending;
  private boolean complete;
  private boolean clearIfNone;

  public ReconParser(Input input) {
    this.input = Objects.requireNonNull(input);
    this.state = new ArrayDeque<>(Collections.singleton(ParseEvents.ParseState.Init));
    this.complete = false;
    this.clearIfNone = false;
  }

  public ReconParser(Input input, Deque<ParseEvents.ParseState> state, Parser<ParserTransition> current, ParseEvents pending, boolean complete, boolean clearIfNone) {
    this.input = input;
    this.state = state;
    this.current = current;
    this.pending = pending;
    this.complete = complete;
    this.clearIfNone = clearIfNone;
  }

  public boolean isError() {
    if (this.complete) {
      return false;
    } else if (this.current == null) {
      return this.input.isError();
    } else {
      return this.current.isCont();
    }
  }

  public boolean isCont() {
    if (this.complete) {
      return false;
    } else if (this.current == null) {
      return this.input.isContinuation();
    } else {
      return this.current.isCont();
    }
  }

  /**
   * Returns whether the input is done.
   *
   * @return if the input is done.
   */
  public boolean isDone() {
    return this.input.isDone();
  }

  /**
   * Return whether there are any pending events available.
   *
   * @return whether there are any pending events available.
   */
  public boolean hasEvents() {
    return !(this.complete && this.pending == null);
  }

  public ReconParser feed(Input input) {
    return new ReconParser(this.input.extend(Objects.requireNonNull(input)), this.state, this.current, this.pending, this.complete, this.clearIfNone);
  }

  public ParseResult<ReadEvent> next() {
    if (input.isDone()) {
//      System.out.println("Current state: " + this.state.peekLast() + ", input done");
    } else {
//      System.out.println("Current state: " + this.state.peekLast() + ", head: " + (char) input.head());
    }

    if (this.pending != null) {
      Optional<EventOrEnd> optEvent = pending.takeEvent();
      if (optEvent.isPresent()) {
        return onEvent(optEvent.get());
      }
    }

    if (this.complete) {
      return ParseResult.end();
    } else if (this.current != null) {
      if (this.current.isError()) {
        return ParseResult.error(((ParserError<?>) this.current).getCause());
      } else if (this.input.isContinuation()) {
        ParseResult<ParseEvents> result = this.feed();

        if (result.isOk()) {
          Optional<EventOrEnd> optEvent = result.bind().takeEvent();
          if (optEvent.isPresent()) {
            return onEvent(optEvent.get());
          }
        } else {
          return result.cast();
        }

        if (this.input.isError()) {
          return ParseResult.error(((InputError) this.input));
        } else if (!this.complete && this.input.isDone()) {
          return ParseResult.error("Not enough data");
        } else {
          return ParseResult.continuation();
        }
      }
    }

    if (this.state.peekLast() != null) {
      while (true) {
        ParseResult<ParseEvents> result = this.nextEvent();
        if (result.isOk()) {
          Optional<EventOrEnd> optEvent = result.bind().takeEvent();
          if (optEvent.isPresent()) {
            return onEvent(optEvent.get());
          }
        } else {
          return result.cast();
        }
      }
    }

    this.complete = true;
    return ParseResult.end();
  }

  private ParseResult<ReadEvent> onEvent(EventOrEnd eventOrEnd) {
    if (eventOrEnd.isEnd()) {
      this.complete = true;
      return ParseResult.end();
    } else {
      Event event = (Event) eventOrEnd;
      this.pending = event.getNext();
      return ParseResult.ok(event.getEvent());
    }
  }

  private ParseResult<ParseEvents> feed() {
    this.current = this.current.feed(this.input);

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

  private ParseResult<ParseEvents> parseEvent(Parser<ParserTransition> parser, boolean clearIfNone) {
    if (this.current == null) {
      this.current = parser;
      this.clearIfNone = clearIfNone;
    }

    return this.feed();
  }

  private ParseResult<ParseEvents> nextEvent() {
    switch (this.state.getLast()) {
      case Init:
        return parseEvent(alt(
            Parser.lambda(input -> {
              if (input.isDone()) {
                return Parser.done(ReadEvent.extant());
              } else {
                return Parser.error("Expected an empty input");
              }
            }).map(ReadEvent::transition),
            preceded(multispace0(), parseInit())
        ), true);
      case AfterAttr:
        if (input.isDone()) {
          this.current = null;
          this.complete = true;
          return ParseResult.ok(ParseEvents.twoEvents(ReadEvent.startBody(), ReadEvent.endRecord()));
        } else {
          return parseEvent(preceded(multispace0(), parseAfterAttr()), false);
        }
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

            if (parseState == ParseEvents.ParseState.RecordBodySlot) {
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
        return parseEvent(preceded(space0(), parseAfterValue(ItemsKind.attr())).map(s -> {
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
    if (stateChange == null) {
      if (clearIfNone) {
        this.state.clear();
      }

      return;
    }

//    System.out.println("Transitioning to: " + stateChange);

    if (stateChange.isNone()) {
      if (clearIfNone) {
        this.state.clear();
      }
    } else if (stateChange.isPopAfterAttr()) {
      this.state.pollLast();
      ParseEvents.ParseState last = this.state.pollLast();
      if (last != null) {
        this.state.addLast(ParseEvents.ParseState.AfterAttr);
      }

//      System.out.println("Pop after attr stack: " + this.state);
    } else if (stateChange.isPopAfterItem()) {
      this.state.pollLast();
      ParseEvents.ParseState parseState = this.state.pollLast();
      if (parseState != null) {
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
      ParseEvents.ParseState last = this.state.pollLast();
      if (last != null) {
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
