package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.events.Event;
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.ChangeState;
import ai.swim.recon.models.state.PushAttrNewRec;
import ai.swim.recon.models.state.StateChange;
import ai.swim.recon.result.ParseResult;
import ai.swim.recon.result.ResultError;

import java.util.*;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.ParserExt.alt;
import static ai.swim.codec.parsers.string.StringExt.multispace0;
import static ai.swim.codec.parsers.string.StringExt.space0;
import static ai.swim.recon.ReconParserParts.*;

/**
 * An incremental recon parser.
 */
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

  private ReconParser(Input input, Deque<ParseEvents.ParseState> state, Parser<ParserTransition> current, ParseEvents pending, boolean complete, boolean clearIfNone) {
    this.input = input;
    this.state = state;
    this.current = current;
    this.pending = pending;
    this.complete = complete;
    this.clearIfNone = clearIfNone;
  }

  /**
   * Returns whether the parser is in an error state.
   */
  public boolean isError() {
    if (this.complete) {
      return false;
    } else if (this.current == null) {
      return this.input.isError();
    } else {
      return this.current.isError();
    }
  }

  /**
   * Returns the cause of the error if this parser is in an error state.
   *
   * @throws IllegalStateException if this parser is not in an error state
   */
  public ResultError<ReadEvent> error() {
    if (this.current != null && this.current.isError()) {
      return new ResultError<>(((ParserError<ParserTransition>) this.current).cause());
    } else if (this.input.isError()) {
      return new ResultError<>(((InputError) this.input).cause());
    } else {
      throw new IllegalStateException("Parser is not in an error state");
    }
  }

  /**
   * Returns whether the parser is in a continuation state.
   */
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
   */
  public boolean isDone() {
    return this.input.isDone();
  }

  /**
   * Return whether there are any pending events available.
   */
  public boolean hasEvents() {
    return !(this.complete && this.pending == null);
  }

  /**
   * Feeds this {@code ReconParser} more data to consume. This does not invoke any parsing operations and instead
   * returns a {@code ReconParser} that contains an extension of any remaining and unconsumed {@code Input} and the
   * provided {@code Input}.
   * <p>
   * The input must be available between multiple invocations of the {@code ReconParser#next} method and must not be
   * modified once it has been provided. Once an inner parser has decided on a parsing branch to take it will strip off
   * any consumed tokens from the {@code input}.
   */
  public ReconParser feed(Input input) {
    if (this.complete) {
      throw new IllegalStateException("Cannot feed a completed parser more data");
    }

    return new ReconParser(this.input.extend(Objects.requireNonNull(input)), this.state, this.current, this.pending, this.complete, this.clearIfNone);
  }

  /**
   * Incrementally parses as much data as possible from the provided {@code Input} and returns a result representing
   * the operation.
   *
   * @return a {@code ParseResult} in one of the following states:
   * - {@code ParseOk}: parsed successfully and an event was produced.
   * - {@code ParseContinuation}: not enough data is available to produce an event.
   * - {@code ParseError}: the {@code Input} is invalid.
   */
  public ParseResult<ReadEvent> next() {
    // If there are any pending events then drain them before attempting to parse anything else
    if (this.pending != null) {
      Optional<Event> optEvent = pending.takeEvent();
      if (optEvent.isPresent()) {
        return onEvent(optEvent.get());
      }
    }

    if (this.complete) {
      return ParseResult.end();
    } else if (this.current != null) {
      // The current parser didn't complete in its last iteration, attempt to feed it more data.
      if (this.current.isError()) {
        return ParseResult.error(((ParserError<?>) this.current).cause());
      } else if (this.input.isContinuation()) {
        ParseResult<ParseEvents> result = this.feed();
        if (result.isOk()) {
          Optional<Event> optEvent = result.bind().takeEvent();
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
      // Continue parsing tokens while the input has remaining tokens.
      while (true) {
        ParseResult<ParseEvents> result = this.nextEvent();
        if (result.isOk()) {
          Optional<Event> optEvent = result.bind().takeEvent();
          if (optEvent.isPresent()) {
            return onEvent(optEvent.get());
          }
        } else {
          return result.cast();
        }
      }
    }

    // At this point, the parsing completed successfully
    this.complete = true;
    return ParseResult.end();
  }

  private ParseResult<ReadEvent> onEvent(Event event) {
    this.pending = event.getNext();
    return ParseResult.ok(event.getEvent());
  }

  private ParseResult<ParseEvents> feed() {
    this.current = this.current.feed(this.input);

    if (this.current.isCont()) {
      return ParseResult.continuation();
    } else if (this.current.isError()) {
      return ParseResult.error(((ParserError<ParserTransition>) this.current).cause());
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
                return Parser.error(input, "Expected an empty input");
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
              return new ParserTransition(ReadEvent.slot());
            } else {
              return new ParserTransition(ParseEvents.noEvent());
            }
          } else {
            this.transition(StateChange.popAfterItem(), false);
            return new ParserTransition(ReadEvent.endRecord());
          }
        }), false);
      case RecordBodyAfterSlot:
        return parseEvent(preceded(space0(), parseAfterSlot(ItemsKind.record())).map(s -> {
          if (s.isPresent()) {
            this.state.removeLast();
            this.state.addLast(s.get());
            return new ParserTransition(ParseEvents.noEvent());
          } else {
            this.transition(StateChange.popAfterItem(), false);
            return new ParserTransition(ReadEvent.endRecord());
          }
        }), false);
      case AttrBodyAfterSlot:
        return parseEvent(preceded(space0(), parseAfterSlot(ItemsKind.attr())).map(s -> {
          if (s.isPresent()) {
            this.state.removeLast();
            this.state.addLast(s.get());
            return new ParserTransition(ParseEvents.noEvent());
          } else {
            this.transition(StateChange.popAfterAttr(), false);
            return new ParserTransition(ReadEvent.endAttribute());
          }
        }), false);
      case AttrBodyAfterValue:
        return parseEvent(preceded(space0(), parseAfterValue(ItemsKind.attr())).map(s -> {
          if (s.isPresent()) {
            ParseEvents.ParseState parseState = s.get();
            this.state.removeLast();
            this.state.addLast(parseState);

            if (parseState == ParseEvents.ParseState.AttrBodySlot) {
              return new ParserTransition(ReadEvent.slot());
            } else {
              return new ParserTransition(ParseEvents.noEvent());
            }
          } else {
            this.transition(StateChange.popAfterAttr(), false);
            return new ParserTransition(ReadEvent.endAttribute());
          }
        }), false);
      case RecordBodySlot:
        return parseEvent(preceded(space0(), parseSlotValue(ItemsKind.record())), false);
      case AttrBodySlot:
        return parseEvent(preceded(space0(), parseSlotValue(ItemsKind.attr())), false);
      default:
        throw new AssertionError();
    }
  }

  private void transition(StateChange stateChange, boolean clearIfNone) {
    if (stateChange == null) {
      if (clearIfNone) {
        this.state.clear();
      }
      return;
    }

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
